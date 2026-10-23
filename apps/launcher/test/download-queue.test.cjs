const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const { createHash } = require('node:crypto');
const bytes = Buffer.from('Space queue fixture');
const sha1 = createHash('sha1').update(bytes).digest('hex');
const file = {
  url: `https://piston-data.mojang.com/v1/objects/${sha1}/client.jar`,
  sha1,
  size: bytes.length,
  relativePath: 'version/client.jar',
};
async function fixture(t) {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'space-queue-'));
  assert.equal(path.dirname(root), os.tmpdir());
  t.after(() => fs.rm(root, { recursive: true, force: true }));
  return { root, ...(await import('../src/main/download-queue.ts')) };
}
test('queue validates conflicts, total size, paths and Windows case collisions before work', async (t) => {
  const { validatePlan } = await fixture(t);
  assert.equal(validatePlan([file, file]).length, 1);
  assert.throws(
    () =>
      validatePlan([file, { ...file, relativePath: 'VERSION/CLIENT.JAR', size: file.size + 1 }]),
    /конфликт/,
  );
  assert.throws(() => validatePlan([file, { ...file, relativePath: 'version' }]), /каталог/);
  assert.throws(
    () => validatePlan([file, { ...file, relativePath: 'VERSION/CLIENT.JAR.PART' }]),
    /временной/,
  );
  assert.throws(
    () => validatePlan([file, { ...file, relativePath: 'version/client.jar.part/nested' }]),
    /временной/,
  );
  assert.throws(() => validatePlan([]));
  assert.throws(
    () =>
      validatePlan(
        Array.from({ length: 5 }, (_, i) => ({
          ...file,
          size: 512 * 1024 * 1024,
          relativePath: `file${i}.jar`,
        })),
      ),
    /2 ГиБ/,
  );
});
test('queue deduplicates files, bounds concurrency and reports only verified completion', async (t) => {
  const { root, downloadPlan } = await fixture(t);
  const files = Array.from({ length: 6 }, (_, i) => ({
    ...file,
    relativePath: `lib${i}/file.jar`,
  }));
  let fetching = 0,
    maxFetching = 0;
  const progress = [];
  const result = await downloadPlan(root, [...files, files[0]], {
    concurrency: 2,
    fetcher: async () => {
      fetching++;
      maxFetching = Math.max(maxFetching, fetching);
      await new Promise((resolve) => setImmediate(resolve));
      fetching--;
      return new Response(bytes);
    },
    onProgress: (status) => progress.push(status),
  });
  assert.equal(result.completedFiles, 6);
  assert.equal(result.verifiedBytes, 6 * bytes.length);
  assert.ok(maxFetching <= 2);
  assert.equal(progress.at(-1).state, 'completed');
  assert.ok(
    progress.every(
      (status) => status.activeFiles <= 2 && status.verifiedBytes <= status.totalBytes,
    ),
  );
});
test('temporary HTTP failures retry with bounded backoff; permanent failures do not retry', async (t) => {
  const { root, downloadPlan } = await fixture(t);
  let calls = 0;
  const waits = [];
  const result = await downloadPlan(root, [file], {
    fetcher: async () => (++calls <= 2 ? new Response('', { status: 503 }) : new Response(bytes)),
    wait: async (ms) => {
      waits.push(ms);
    },
  });
  assert.equal(calls, 3);
  assert.equal(result.retryCount, 2);
  assert.deepEqual(waits, [250, 500]);
  let permanentCalls = 0;
  await assert.rejects(
    downloadPlan(root, [{ ...file, relativePath: 'missing.jar' }], {
      fetcher: async () => {
        permanentCalls++;
        return new Response('', { status: 404 });
      },
      wait: async () => {},
    }),
    /HTTP 404/,
  );
  assert.equal(permanentCalls, 1);
});
test('bad hash retries from clean partial data but never exceeds the attempt budget', async (t) => {
  const { root, downloadPlan } = await fixture(t);
  let calls = 0;
  const result = await downloadPlan(root, [file], {
    fetcher: async () => new Response(++calls === 1 ? Buffer.alloc(bytes.length) : bytes),
    wait: async () => {},
  });
  assert.equal(result.completedFiles, 1);
  assert.equal(calls, 2);
  let failures = 0;
  await assert.rejects(
    downloadPlan(root, [{ ...file, relativePath: 'bad.jar' }], {
      fetcher: async () => {
        failures++;
        return new Response('', { status: 503 });
      },
      wait: async () => {},
    }),
    /HTTP 503/,
  );
  assert.equal(failures, 3);
});
test('cancel stops new work and a failed plan never reports completion', async (t) => {
  const { root, downloadPlan } = await fixture(t);
  const controller = new AbortController();
  const progress = [];
  let calls = 0;
  await assert.rejects(
    downloadPlan(root, [file, { ...file, relativePath: 'next.jar' }], {
      concurrency: 1,
      signal: controller.signal,
      fetcher: async () => {
        calls++;
        return new Response(bytes);
      },
      onProgress: (status) => {
        progress.push(status);
        if (status.receivedBytes > 0) controller.abort();
      },
    }),
    { name: 'AbortError' },
  );
  assert.equal(calls, 1);
  assert.equal(progress.at(-1).state, 'cancelled');
  assert.equal(
    progress.some((status) => status.state === 'completed'),
    false,
  );
  const failures = [];
  await assert.rejects(
    downloadPlan(root, [{ ...file, relativePath: 'fail.jar' }], {
      retries: 0,
      fetcher: async () => new Response('', { status: 403 }),
      onProgress: (status) => failures.push(status),
    }),
    /HTTP 403/,
  );
  assert.equal(failures.at(-1).state, 'failed');
});
test('observer errors do not abandon asynchronous writes or turn a verified plan into a failure', async (t) => {
  const { root, downloadPlan } = await fixture(t);
  const result = await downloadPlan(root, [file], {
    fetcher: async () => new Response(bytes),
    onProgress: () => {
      throw new Error('UI observer disconnected');
    },
  });
  assert.equal(result.completedFiles, 1);
  assert.deepEqual(await fs.readFile(path.join(root, file.relativePath)), bytes);
});
