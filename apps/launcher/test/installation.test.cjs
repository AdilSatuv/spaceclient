const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const file = {
  relativePath: 'libraries/test-natives-windows.jar',
  size: 10,
  sha1: 'a'.repeat(40),
  url: 'https://libraries.minecraft.net/test-natives-windows.jar',
};
const plan = {
  version: '1.8.9',
  platform: 'windows-x64',
  files: [file],
  natives: [{ archive: file.relativePath, exclude: ['META-INF/'] }],
  totalBytes: 10,
  blockers: ['Not a runnable Space build'],
  launchReady: false,
};
const spacePin = {
  schemaVersion: 1,
  channel: 'development',
  modVersion: '0.1.0',
  minecraftVersion: '1.8.9',
  loaderVersion: '0.18.3',
  gameSourceTree: 'a'.repeat(40),
  sha256: 'b'.repeat(64),
  size: 20,
};
async function fixture(t, overrides = {}) {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'space-install-'));
  assert.equal(path.dirname(root), os.tmpdir());
  t.after(() => fs.rm(root, { recursive: true, force: true }));
  const { InstallationService } = await import('../src/main/installation.ts');
  const service = new InstallationService(
    async (id) => {
      const target = path.join(root, id);
      await fs.mkdir(target, { recursive: true });
      return target;
    },
    {
      prepare: async () => structuredClone(plan),
      availableBytes: async () => 1024 ** 3,
      download: async (_root, _files, options) => {
        options.onProgress({
          state: 'running',
          completedFiles: 1,
          totalFiles: 1,
          receivedBytes: 10,
          verifiedBytes: 10,
          totalBytes: 10,
          retryCount: 1,
        });
      },
      extract: async (target) => ({
        directory: path.join(target, 'natives/run-fixture'),
        files: [],
        totalBytes: 0,
      }),
      ...overrides,
    },
  );
  return { root, service };
}
test('installation lifecycle records only fully prepared base files and returns detached status', async (t) => {
  const { root, service } = await fixture(t);
  assert.equal(service.status('alpha').phase, 'idle');
  const first = service.start('alpha');
  assert.equal(first.phase, 'planning');
  first.phase = 'prepared';
  assert.equal(service.status('alpha').phase, 'planning');
  await service.settled();
  const result = service.status('alpha');
  assert.equal(result.phase, 'prepared');
  assert.equal(result.verifiedBytes, 10);
  assert.equal(result.retryCount, 1);
  assert.equal(service.running, false);
  const receipt = JSON.parse(
    await fs.readFile(path.join(root, 'alpha/base-installation.json'), 'utf8'),
  );
  assert.equal(receipt.launchReady, false);
  assert.equal(receipt.kind, 'minecraft-base-only');
  assert.equal(receipt.nativeDirectory, 'natives/run-fixture');
  assert.deepEqual(receipt.blockers, plan.blockers);
  assert.equal(
    (await fs.readdir(path.join(root, 'alpha'))).some((name) => name.endsWith('.tmp')),
    false,
  );
});
test('a verified Space bundle extends counts and records development identity without enabling launch', async (t) => {
  const { root, service } = await fixture(t, {
    space: {
      pin: spacePin,
      install: async () => ({ ...spacePin, relativePath: 'mods/space-client.jar' }),
    },
  });
  service.start('alpha');
  await service.settled();
  const status = service.status('alpha');
  assert.equal(status.phase, 'prepared');
  assert.equal(status.spaceVersion, '0.1.0');
  assert.equal(status.totalFiles, 2);
  assert.equal(status.completedFiles, 2);
  assert.equal(status.totalBytes, 30);
  assert.equal(status.verifiedBytes, 30);
  const receipt = JSON.parse(
    await fs.readFile(path.join(root, 'alpha/base-installation.json'), 'utf8'),
  );
  assert.equal(receipt.kind, 'space-development');
  assert.equal(receipt.space.gameSourceTree, spacePin.gameSourceTree);
  assert.equal(receipt.launchReady, false);
});
test('Space copy failure does not publish a new receipt or report the mod as installed', async (t) => {
  const { root, service } = await fixture(t, {
    space: {
      pin: spacePin,
      install: async () => {
        throw new Error('bundle mismatch');
      },
    },
  });
  await fs.mkdir(path.join(root, 'alpha'));
  await fs.writeFile(path.join(root, 'alpha/base-installation.json'), 'old receipt');
  service.start('alpha');
  await service.settled();
  assert.equal(service.status('alpha').phase, 'error');
  assert.equal(service.status('alpha').spaceVersion, null);
  assert.equal(
    await fs.readFile(path.join(root, 'alpha/base-installation.json'), 'utf8'),
    'old receipt',
  );
});
test('cancelling the Space installation stage waits for its abort and publishes no success', async (t) => {
  let began;
  const started = new Promise((resolve) => {
    began = resolve;
  });
  const { root, service } = await fixture(t, {
    space: {
      pin: spacePin,
      install: async (_root, signal) => {
        began();
        await new Promise((_resolve, reject) =>
          signal.addEventListener('abort', () => reject(signal.reason), { once: true }),
        );
      },
    },
  });
  service.start('alpha');
  await started;
  assert.equal(service.status('alpha').phase, 'installing');
  await service.cancel('alpha');
  assert.equal(service.status('alpha').phase, 'cancelled');
  await assert.rejects(fs.stat(path.join(root, 'alpha/base-installation.json')), {
    code: 'ENOENT',
  });
});
test('a global job lock covers asynchronous root setup; invalid IDs are rejected', async (t) => {
  const { service } = await fixture(t);
  assert.throws(() => service.start('../escape'));
  service.start('alpha');
  assert.throws(() => service.start('alpha'), /Уже/);
  assert.throws(() => service.start('beta'), /Уже/);
  await service.settled();
  assert.equal(service.status('beta').phase, 'idle');
});
test('download/extraction failures never overwrite the last successful receipt and can retry', async (t) => {
  for (const failing of ['download', 'extract']) {
    let fail = true;
    const { root, service } = await fixture(t, {
      [failing]: async (target) => {
        if (fail) throw new Error('fixture failure');
        return { directory: path.join(target, 'natives/run-retry'), files: [], totalBytes: 0 };
      },
    });
    await fs.mkdir(path.join(root, 'alpha'));
    await fs.writeFile(path.join(root, 'alpha/base-installation.json'), 'previous receipt');
    service.start('alpha');
    await service.settled();
    assert.equal(service.status('alpha').phase, 'error');
    assert.equal(
      await fs.readFile(path.join(root, 'alpha/base-installation.json'), 'utf8'),
      'previous receipt',
    );
    fail = false;
    service.start('alpha');
    await service.settled();
    assert.equal(service.status('alpha').phase, 'prepared');
  }
});
test('low disk space stops before bulk downloads or native extraction', async (t) => {
  let calls = 0;
  const { service } = await fixture(t, {
    availableBytes: async () => 1,
    download: async () => {
      calls++;
    },
    extract: async () => {
      calls++;
    },
  });
  service.start('alpha');
  await service.settled();
  assert.equal(service.status('alpha').phase, 'error');
  assert.match(service.status('alpha').message, /места/);
  assert.equal(calls, 0);
});
test('cancellation aborts active work, waits for settlement and leaves no successful receipt', async (t) => {
  let began;
  const started = new Promise((resolve) => {
    began = resolve;
  });
  const { root, service } = await fixture(t, {
    download: async (_root, _files, { signal }) => {
      began();
      await new Promise((_resolve, reject) =>
        signal.addEventListener('abort', () => reject(signal.reason), { once: true }),
      );
    },
  });
  service.start('alpha');
  await started;
  assert.equal((await service.cancel('beta')).phase, 'idle');
  assert.equal(service.running, true);
  const cancelled = await service.cancel('alpha');
  assert.equal(cancelled.phase, 'cancelled');
  assert.equal(service.running, false);
  await assert.rejects(fs.stat(path.join(root, 'alpha/base-installation.json')), {
    code: 'ENOENT',
  });
});
test('shutdown cancels metadata preparation before releasing its job lock', async (t) => {
  let began;
  const started = new Promise((resolve) => {
    began = resolve;
  });
  const { service } = await fixture(t, {
    prepare: async (_root, { signal }) => {
      began();
      await new Promise((_resolve, reject) =>
        signal.addEventListener('abort', () => reject(signal.reason), { once: true }),
      );
    },
  });
  service.start('alpha');
  await started;
  await service.shutdown();
  assert.equal(service.status('alpha').phase, 'cancelled');
  assert.equal(service.running, false);
});
