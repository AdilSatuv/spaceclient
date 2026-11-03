const { test } = require('node:test'),
  assert = require('node:assert/strict'),
  fs = require('node:fs/promises'),
  path = require('node:path'),
  os = require('node:os');
async function fixture(t, writer) {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'space-recovery-'));
  t.after(() => fs.rm(root, { recursive: true, force: true }));
  const { ProfileStore } = await import('../src/main/store.ts'),
    { ProfileRecovery, RECOVERY_FILES } = await import('../src/main/profile-recovery.ts');
  const profiles = await ProfileStore.open(root, 8192);
  await profiles.saveNickname('SecretNickname');
  await profiles.setJava('default', 'C:\\PRIVATE\\java.exe');
  const instance = path.join(await profiles.instanceDirectory('default'), 'minecraft');
  await fs.mkdir(path.join(instance, 'config'), { recursive: true });
  await fs.mkdir(path.join(instance, 'saves/world'), { recursive: true });
  await fs.writeFile(path.join(instance, 'saves/world/level.dat'), 'WORLD');
  for (const file of RECOVERY_FILES)
    await fs.writeFile(path.join(instance, file), 'original:' + file);
  await fs.writeFile(path.join(instance, 'config/space-appearance.properties'), 'PUBLIC_ACCOUNT');
  let report;
  const recovery = new ProfileRecovery(
    root,
    {
      profiles,
      installation: () => ({
        phase: 'idle',
        completedFiles: 0,
        totalFiles: 0,
        retryCount: 0,
        message: 'SECRET',
      }),
      launch: () => ({ phase: 'idle', exitCode: null, log: ['SECRET'] }),
      build: null,
      exportReport: async (bytes) => {
        report = bytes;
        return true;
      },
    },
    writer,
  );
  return {
    root,
    instance,
    profiles,
    recovery,
    files: RECOVERY_FILES,
    get report() {
      return report;
    },
  };
}
test('profile backup/restore keeps exactly the six settings files, memory and name; worlds/accounts/Java remain unchanged', async (t) => {
  const f = await fixture(t);
  await f.recovery.backup('default');
  await fs.writeFile(path.join(f.instance, 'options.txt'), 'changed');
  await f.profiles.save({ id: 'default', name: 'Changed', memoryMb: 3072 });
  await f.recovery.restore('default');
  for (const file of f.files)
    assert.equal(await fs.readFile(path.join(f.instance, file), 'utf8'), 'original:' + file);
  assert.equal(await fs.readFile(path.join(f.instance, 'saves/world/level.dat'), 'utf8'), 'WORLD');
  assert.equal(
    await fs.readFile(path.join(f.instance, 'config/space-appearance.properties'), 'utf8'),
    'PUBLIC_ACCOUNT',
  );
  assert.equal(f.profiles.profile('default').memoryMb, 2048);
  assert.equal(f.profiles.profile('default').javaPath, 'C:\\PRIVATE\\java.exe');
  assert.equal(f.profiles.snapshot().localNickname, 'SecretNickname');
  const backup = await fs.readFile(path.join(f.root, 'recovery/default.json'), 'utf8');
  assert.doesNotMatch(backup, /SECRET|PRIVATE|SecretNickname|level.dat|space-appearance/);
});
test('restore rejects corruption, another profile and unknown file keys before mutation', async (t) => {
  const f = await fixture(t);
  await f.recovery.backup('default');
  const file = path.join(f.root, 'recovery/default.json'),
    backup = JSON.parse(await fs.readFile(file, 'utf8'));
  backup.content.files['saves/world/level.dat'] = 'ZXZpbA==';
  await fs.writeFile(file, JSON.stringify(backup));
  await assert.rejects(f.recovery.restore('default'), /повреждена/);
  assert.equal(
    await fs.readFile(path.join(f.instance, 'options.txt'), 'utf8'),
    'original:options.txt',
  );
  await assert.rejects(f.recovery.restore('missing'));
});
test('restore rolls back the complete file set when a write fails midway and preserves backup bytes', async (t) => {
  const { atomicFile } = await import('../src/main/bounded-files.ts');
  let writes = 0;
  const f = await fixture(t, async (...args) => {
    if (++writes === 3) throw new Error('simulated disk failure');
    return atomicFile(...args);
  });
  await f.recovery.backup('default');
  for (const file of f.files) await fs.writeFile(path.join(f.instance, file), 'new:' + file);
  const backup = await fs.readFile(path.join(f.root, 'recovery/default.json'));
  await assert.rejects(f.recovery.restore('default'));
  for (const file of f.files)
    assert.equal(await fs.readFile(path.join(f.instance, file), 'utf8'), 'new:' + file);
  assert.deepEqual(await fs.readFile(path.join(f.root, 'recovery/default.json')), backup);
});
test('diagnostic export contains no player identity, paths, account/session content or raw logs', async (t) => {
  const f = await fixture(t);
  await f.recovery.diagnostics('default');
  const report = f.report.toString('utf8');
  assert.doesNotMatch(
    report,
    /SecretNickname|SECRET|PRIVATE|java.exe|dataRoot|log|sessionToken|username/,
  );
  assert.equal(JSON.parse(report).profile.javaSelected, true);
  assert.ok(f.report.length < 4096);
});
test('linked or oversized settings are refused before backup writes', async (t) => {
  const f = await fixture(t);
  await fs.writeFile(path.join(f.instance, 'options.txt'), Buffer.alloc(512 * 1024 + 1));
  await assert.rejects(f.recovery.backup('default'));
  await fs.unlink(path.join(f.instance, 'options.txt'));
  await fs.symlink(
    path.join(f.instance, 'saves/world/level.dat'),
    path.join(f.instance, 'options.txt'),
  );
  await assert.rejects(f.recovery.backup('default'));
  await assert.rejects(fs.readFile(path.join(f.root, 'recovery/default.json')));
});
