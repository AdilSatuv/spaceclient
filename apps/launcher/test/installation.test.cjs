const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const { createHash } = require('node:crypto');
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

test('installation status exposes intentional messages but redacts unexpected dependency errors', async (t) => {
  const { UserFacingError } = await import('../src/main/application/user-facing-error.ts');
  for (const failure of [
    new Error('C:/private/account token=secret'),
    new UserFacingError('Повторите загрузку.'),
  ]) {
    const { service } = await fixture(t, {
      prepare: async () => {
        throw failure;
      },
    });
    service.start('alpha');
    await service.settled();
    const status = service.status('alpha');
    assert.equal(status.phase, 'error');
    assert.equal(
      status.message,
      failure instanceof UserFacingError ? failure.message : 'Не удалось подготовить файлы.',
    );
    assert.doesNotMatch(JSON.stringify(status), /private|secret/);
  }
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

test('shutdown permanently closes installation admission even when no job was active', async (t) => {
  const { root, service } = await fixture(t);
  await service.shutdown();
  assert.throws(() => service.start('alpha'), /завершает работу/);
  assert.equal(service.running, false);
  assert.equal(service.status('alpha').phase, 'idle');
  assert.deepEqual(await fs.readdir(root), []);
});

test('shutdown blocks a new repair after the cancelled job releases its reservation', async (t) => {
  let began;
  const started = new Promise((resolve) => {
    began = resolve;
  });
  let prepareCalls = 0;
  const { root, service } = await fixture(t, {
    prepare: async (_root, { signal }) => {
      prepareCalls++;
      began();
      await new Promise((_resolve, reject) =>
        signal.addEventListener('abort', () => reject(signal.reason), { once: true }),
      );
    },
  });
  service.start('alpha');
  await started;
  const closing = service.shutdown();
  assert.throws(() => service.start('beta'), /завершает работу/);
  await closing;
  assert.throws(() => service.start('alpha'), /завершает работу/);
  assert.equal(prepareCalls, 1);
  assert.equal(service.running, false);
  assert.equal(service.status('alpha').phase, 'cancelled');
  await assert.rejects(fs.stat(path.join(root, 'alpha/base-installation.json')), {
    code: 'ENOENT',
  });
});

test('low disk guidance states the actual plan requirement and preserves the previous receipt', async (t) => {
  let bulkCalls = 0;
  const { root, service } = await fixture(t, {
    prepare: async () => ({ ...structuredClone(plan), totalBytes: 512 * 1024 * 1024 + 1 }),
    availableBytes: async () => 400 * 1024 * 1024,
    download: async () => {
      bulkCalls++;
    },
  });
  await fs.mkdir(path.join(root, 'alpha'));
  const receipt = path.join(root, 'alpha/base-installation.json');
  await fs.writeFile(receipt, 'previous receipt');
  service.start('alpha');
  await service.settled();
  assert.equal(service.status('alpha').phase, 'error');
  assert.match(service.status('alpha').message, /673 МиБ/);
  assert.match(service.status('alpha').message, /повторите/);
  assert.equal(bulkCalls, 0);
  assert.equal(await fs.readFile(receipt, 'utf8'), 'previous receipt');
});

test('filesystem failures explain a safe recovery action, preserve receipt and permit retry', async (t) => {
  for (const [stage, code, cause, action] of [
    ['prepare', 'ENOSPC', /места|квота/, /Освободите/],
    ['download', 'EDQUOT', /места|квота/, /Освободите/],
    ['download', 'EACCES', /доступа/, /разрешение записи/],
    ['extract', 'EPERM', /доступа/, /разрешение записи/],
    ['extract', 'EBUSY', /занят/, /Закройте программы/],
    ['extract', 'ETXTBSY', /занят/, /Закройте программы/],
  ]) {
    let fail = true;
    const { root, service } = await fixture(t, {
      [stage]: async (target) => {
        if (fail)
          throw Object.assign(new Error('SECRET-token C:/private/user --accessToken SECRET'), {
            code,
            cause: new Error('SECRET-nested'),
          });
        return stage === 'prepare'
          ? structuredClone(plan)
          : {
              directory: path.join(target, 'natives/run-retry'),
              files: [],
              totalBytes: 0,
            };
      },
    });
    await fs.mkdir(path.join(root, 'alpha'));
    const receipt = path.join(root, 'alpha/base-installation.json');
    await fs.writeFile(receipt, 'previous receipt');
    service.start('alpha');
    await service.settled();
    const status = service.status('alpha');
    assert.equal(status.phase, 'error');
    assert.match(status.message, cause, code);
    assert.match(status.message, action, code);
    assert.match(status.message, /повторите/, code);
    assert.doesNotMatch(JSON.stringify(status), /SECRET|private|accessToken/);
    assert.equal(await fs.readFile(receipt, 'utf8'), 'previous receipt');
    assert.equal(service.running, false);
    fail = false;
    service.start('alpha');
    await service.settled();
    assert.equal(service.status('alpha').phase, 'prepared');
    assert.equal(JSON.parse(await fs.readFile(receipt, 'utf8')).launchReady, false);
  }
});

test('ordinary repair cancellation can resume a partial while preserving verified files and the old receipt', async (t) => {
  const { downloadPlan } = await import('../src/main/download-queue.ts');
  const cached = Buffer.from('verified cached library');
  const missing = Buffer.from('library requiring repair');
  const files = [cached, missing].map((bytes, index) => {
    const sha1 = createHash('sha1').update(bytes).digest('hex');
    return {
      relativePath: `libraries/library-${index}.jar`,
      size: bytes.length,
      sha1,
      url: `https://piston-data.mojang.com/v1/objects/${sha1}/library.jar`,
    };
  });
  let cancelOnce = true;
  let cancelling;
  const requests = [];
  const { root, service } = await fixture(t, {
    prepare: async () => ({
      ...structuredClone(plan),
      files,
      natives: [],
      totalBytes: cached.length + missing.length,
    }),
    download: async (target, input, options) =>
      downloadPlan(target, input, {
        ...options,
        concurrency: 1,
        retries: 0,
        fetcher: async (url, { headers }) => {
          requests.push({ url, range: headers.Range });
          assert.equal(url, files[1].url, 'the verified cache must not be downloaded again');
          if (headers.Range) {
            assert.equal(headers.Range, 'bytes=7-');
            return new Response(missing.subarray(7), {
              status: 206,
              headers: { 'Content-Range': `bytes 7-${missing.length - 1}/${missing.length}` },
            });
          }
          let sent = false;
          return new Response(
            new ReadableStream({
              pull(stream) {
                if (sent) stream.close();
                else {
                  sent = true;
                  stream.enqueue(missing.subarray(0, 7));
                }
              },
            }),
          );
        },
        onProgress: (status) => {
          options.onProgress(status);
          if (cancelOnce && status.receivedBytes >= cached.length + 7) {
            cancelOnce = false;
            cancelling = service.cancel('alpha');
          }
        },
      }),
  });
  const instance = path.join(root, 'alpha');
  await fs.mkdir(path.join(instance, 'libraries'), { recursive: true });
  const cachedPath = path.join(instance, files[0].relativePath);
  const repairedPath = path.join(instance, files[1].relativePath);
  const receipt = path.join(instance, 'base-installation.json');
  await fs.writeFile(cachedPath, cached);
  const cachedStat = await fs.stat(cachedPath);
  await fs.writeFile(repairedPath, 'corrupt existing file');
  await fs.writeFile(receipt, 'previous receipt');
  service.start('alpha');
  await service.settled();
  await cancelling;
  assert.equal(service.status('alpha').phase, 'cancelled');
  assert.equal(service.running, false);
  assert.equal(await fs.readFile(receipt, 'utf8'), 'previous receipt');
  assert.deepEqual(await fs.readFile(repairedPath + '.part'), missing.subarray(0, 7));
  assert.equal(await fs.readFile(repairedPath, 'utf8'), 'corrupt existing file');
  service.start('alpha');
  await service.settled();
  assert.equal(service.status('alpha').phase, 'prepared');
  assert.equal(service.status('alpha').completedFiles, 2);
  assert.equal(requests.length, 2);
  assert.equal(requests[0].range, undefined);
  assert.equal(requests[1].range, 'bytes=7-');
  assert.deepEqual(await fs.readFile(cachedPath), cached);
  assert.equal((await fs.stat(cachedPath)).mtimeMs, cachedStat.mtimeMs);
  assert.deepEqual(await fs.readFile(repairedPath), missing);
  await assert.rejects(fs.stat(repairedPath + '.part'), { code: 'ENOENT' });
  assert.equal(JSON.parse(await fs.readFile(receipt, 'utf8')).launchReady, false);
});
