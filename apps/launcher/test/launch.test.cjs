const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const { EventEmitter } = require('node:events');
const { PassThrough } = require('node:stream');
const { createHash } = require('node:crypto');
const profile = {
  id: 'default',
  name: 'Orbit',
  memoryMb: 2048,
  javaPath: 'C:\\Runtime with spaces\\bin\\java.exe',
};
const signal = () => new AbortController().signal;
async function service(overrides = {}) {
  const { LaunchService } = await import('../src/main/launch.ts');
  const calls = [];
  const children = [];
  const value = new LaunchService(async (id) => '/instances/' + id, null, {
    prepare: async (root, p, name) => ({
      executable: p.javaPath,
      args: ['--username', name],
      cwd: root,
      env: {},
    }),
    spawn: (...args) => {
      calls.push(args);
      const child = new EventEmitter();
      child.stdout = new PassThrough();
      child.stderr = new PassThrough();
      children.push(child);
      queueMicrotask(() => child.emit('spawn'));
      return child;
    },
    ...overrides,
  });
  const tick = () => new Promise((resolve) => setImmediate(resolve));
  return { value, calls, children, tick };
}
test('launch reservation covers async preflight, duplicate commands and process close; restart resets exit state', async () => {
  const { value, calls, children, tick } = await service();
  assert.equal(value.start(profile, 'SpaceTester').phase, 'checking');
  assert.throws(() => value.start(profile, 'SpaceTester'), /уже/);
  await tick();
  assert.equal(value.status('default').phase, 'running');
  assert.equal(calls.length, 1);
  assert.equal(calls[0][2].shell, false);
  assert.equal(calls[0][2].windowsHide, false);
  assert.throws(() => value.start(profile, 'SpaceTester'), /уже/);
  children[0].emit('close', 7, null);
  await value.settled();
  assert.equal(value.status('default').exitCode, 7);
  assert.equal(value.status('default').phase, 'error');
  value.start(profile, 'SpaceTester');
  await tick();
  children[1].emit('close', 0, null);
  await value.settled();
  assert.equal(value.status('default').phase, 'exited');
  assert.equal(value.status('default').exitCode, 0);
});
test('preflight and spawn errors release reservation and redact raw errors, including spawn throw', async () => {
  for (const overrides of [
    {
      prepare: async () => {
        throw new Error('SECRET-token path');
      },
    },
    {
      spawn: () => {
        throw new Error('SECRET-token path');
      },
    },
    {
      spawn: () => {
        const c = new EventEmitter();
        queueMicrotask(() => {
          c.emit('error', new Error('SECRET'));
          c.emit('close', -4058, null);
        });
        return c;
      },
    },
  ]) {
    const { value } = await service(overrides);
    value.start(profile, 'SpaceTester');
    await value.settled();
    assert.equal(value.running, false);
    assert.equal(value.status('default').phase, 'error');
    assert.ok(!JSON.stringify(value.status('default')).includes('SECRET'));
    assert.doesNotThrow(() => value.start(profile, 'SpaceTester'));
    await value.settled();
  }
});
test('output flooding produces bounded fixed diagnostics with no raw output, nickname, path or token', async () => {
  const { value, children, tick } = await service();
  value.start(profile, 'SpaceTester');
  await tick();
  for (let i = 0; i < 150; i++)
    children[0].stderr.write(
      Buffer.from('Exception in thread SECRET SpaceTester C:\\private\\token ' + 'x'.repeat(16000)),
    );
  const detached = value.status('default');
  assert.ok(detached.log.length < 60);
  assert.ok(!JSON.stringify(detached).includes('SECRET'));
  assert.ok(!JSON.stringify(detached).includes('SpaceTester'));
  detached.log.push('poison');
  assert.ok(!value.status('default').log.includes('poison'));
  children[0].emit('close', null, 'SIGTERM');
  await value.settled();
  assert.equal(value.status('default').phase, 'error');
  assert.equal(value.status('default').exitCode, null);
});
test('per-profile processes are independent and shutdown waits for game close without killing a world', async () => {
  const { value, children, tick } = await service();
  value.start(profile, 'SpaceTester');
  value.start({ ...profile, id: 'second' }, 'SpaceTester');
  await tick();
  let finished = false;
  const closing = value.shutdown().then(() => {
    finished = true;
  });
  await tick();
  assert.equal(finished, false);
  children[0].emit('close', 0, null);
  await tick();
  assert.equal(finished, false);
  children[1].emit('close', 0, null);
  await closing;
  assert.throws(() => value.start(profile, 'SpaceTester'), /завершает/);
});
test('shutdown cancels preflight before spawn and never starts an unvalidated nickname', async () => {
  let calls = 0;
  const { value } = await service({
    prepare: async (root, p, n, pin, s) => {
      await new Promise((resolve, reject) =>
        s.addEventListener('abort', () => reject(s.reason), { once: true }),
      );
    },
    spawn: () => {
      calls++;
    },
  });
  for (const name of [null, '', 'ab', 'a b', '-Xmx8G', '../abc', 'Юзер', 'a'.repeat(17)])
    assert.throws(() => value.start(profile, name));
  value.start(profile, 'Valid_123');
  await new Promise((resolve) => setImmediate(resolve));
  await value.shutdown();
  assert.equal(calls, 0);
  assert.equal(value.running, false);
});
async function installation(t) {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'space-local-plan-'));
  assert.equal(path.dirname(root), os.tmpdir());
  t.after(() => fs.rm(root, { recursive: true, force: true }));
  const bytes = Buffer.from('verified artifact');
  const artifact = {
    url: 'https://libraries.minecraft.net/example/test.jar',
    relativePath: 'libraries/test.jar',
    size: bytes.length,
    sha1: createHash('sha1').update(bytes).digest('hex'),
  };
  await fs.mkdir(path.join(root, 'libraries'));
  await fs.writeFile(path.join(root, artifact.relativePath), bytes);
  await fs.mkdir(path.join(root, 'mods'));
  await fs.writeFile(path.join(root, 'mods/space-client.jar'), 'mod');
  const base = path.join(root, 'natives');
  const directory = path.join(base, 'run-test');
  await fs.mkdir(directory, { recursive: true });
  await fs.writeFile(path.join(directory, 'test.dll'), bytes);
  const { nativeCacheKey, publishNativeCache } = await import('../src/main/native-cache.ts');
  await publishNativeCache(base, nativeCacheKey([artifact]), directory, [
    {
      relativePath: 'test.dll',
      size: bytes.length,
      sha256: createHash('sha256').update(bytes).digest('hex'),
    },
  ]);
  const pin = { size: 3, sha256: 'a'.repeat(64), gameSourceTree: 'b'.repeat(40) };
  const plan = {
    files: [artifact],
    classpath: [artifact.relativePath, 'versions/1.8.9/1.8.9.jar'],
    natives: [{ archive: artifact.relativePath }],
    mainClass: 'net.fabricmc.loader.impl.launch.knot.KnotClient',
  };
  const dependencies = {
    runtime: async () => ({ usable: true }),
    version: async () => plan,
    space: async () => ({ manifest: pin }),
  };
  const { prepareLocalLaunch } = await import('../src/main/launch-plan.ts');
  return {
    root,
    pin,
    dependencies,
    prepare: () => prepareLocalLaunch(root, profile, 'SpaceTester', pin, signal(), dependencies),
    prepareLocalLaunch,
  };
}
test('verified local plan owns fixed JVM/RAM/classpath/natives arguments and offline identity, with no shell string', async (t) => {
  const { prepare, root } = await installation(t);
  const plan = await prepare();
  assert.equal(plan.executable, profile.javaPath);
  assert.equal(plan.cwd, root);
  assert.ok(plan.args.includes('-Xmx2048M'));
  assert.ok(plan.args.includes('-Djava.library.path=' + path.join(root, 'natives/run-test')));
  assert.ok(
    plan.args[plan.args.indexOf('-cp') + 1].includes(path.join(root, 'libraries/test.jar')),
  );
  assert.equal(plan.args[plan.args.indexOf('--accessToken') + 1], '0');
  const { offlineUuid } = await import('../src/main/launch-plan.ts');
  assert.equal(offlineUuid('Notch'), 'b50ad385829d3141a2167e7d7539ba7f');
  assert.notEqual(offlineUuid('Notch'), offlineUuid('notch'));
});
test('changed/missing classpath bytes, extra mods, bundle mismatch, native corruption and junctions block launch', async (t) => {
  const f = await installation(t);
  const library = path.join(f.root, 'libraries/test.jar');
  await fs.writeFile(library, 'bad');
  await assert.rejects(f.prepare(), /целостности/);
  await fs.writeFile(library, 'verified artifact');
  await fs.writeFile(path.join(f.root, 'mods/extra.jar'), 'other');
  await assert.rejects(f.prepare(), /дополнительные/);
  await fs.unlink(path.join(f.root, 'mods/extra.jar'));
  f.dependencies.space = async () => ({ manifest: { ...f.pin, sha256: 'c'.repeat(64) } });
  await assert.rejects(f.prepare(), /не совпадает/);
  f.dependencies.space = async () => ({ manifest: f.pin });
  await fs.writeFile(path.join(f.root, 'natives/run-test/test.dll'), 'bad');
  await assert.rejects(f.prepare(), /Natives/);
  await fs.unlink(library);
  await assert.rejects(f.prepare(), /целостности/);
  const outside = path.join(f.root, 'outside');
  await fs.mkdir(outside);
  await fs.rename(path.join(f.root, 'libraries'), path.join(f.root, 'old-libraries'));
  await fs.symlink(
    outside,
    path.join(f.root, 'libraries'),
    process.platform === 'win32' ? 'junction' : 'dir',
  );
  await assert.rejects(f.prepare(), /ссылку/);
});
test('missing bundle/runtime and unsuitable Java fail before verification or spawn; environment options are stripped', async (t) => {
  const f = await installation(t);
  await assert.rejects(
    f.prepareLocalLaunch(f.root, profile, 'SpaceTester', null, signal(), f.dependencies),
    /нет Space/,
  );
  await assert.rejects(
    f.prepareLocalLaunch(
      f.root,
      { ...profile, javaPath: null },
      'SpaceTester',
      f.pin,
      signal(),
      f.dependencies,
    ),
    /Выберите Java/,
  );
  f.dependencies.runtime = async () => ({ usable: false, message: 'Choose Java 21' });
  await assert.rejects(f.prepare(), /Choose Java 21/);
  const { javaEnvironment } = await import('../src/main/java.ts');
  const old = process.env.JAVA_TOOL_OPTIONS;
  process.env.JAVA_TOOL_OPTIONS = '-javaagent:bad.jar';
  try {
    assert.equal(javaEnvironment().JAVA_TOOL_OPTIONS, undefined);
  } finally {
    if (old === undefined) delete process.env.JAVA_TOOL_OPTIONS;
    else process.env.JAVA_TOOL_OPTIONS = old;
  }
});
