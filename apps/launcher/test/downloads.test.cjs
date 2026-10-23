const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const os = require('node:os');
const { createHash } = require('node:crypto');
const bytes = Buffer.from('Space Client download test payload');
const sha1 = createHash('sha1').update(bytes).digest('hex');
const artifact = {
  url: `https://launcher.mojang.com/v1/objects/${sha1}/client.jar`,
  sha1,
  size: bytes.length,
  relativePath: 'versions/1.8.9/client.jar',
};
async function fixture(t) {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'space-download-'));
  assert.equal(path.dirname(root), os.tmpdir());
  t.after(() => fs.rm(root, { recursive: true, force: true }));
  return { root, ...(await import('../src/main/downloads.ts')) };
}
test('verified file is committed atomically and reused without another request', async (t) => {
  const { root, downloadArtifact } = await fixture(t);
  let requests = 0;
  const fetcher = async (_url, options) => {
    requests++;
    assert.equal(options.redirect, 'error');
    assert.equal(options.headers['Accept-Encoding'], 'identity');
    return new Response(bytes, { headers: { 'Content-Length': String(bytes.length) } });
  };
  const first = await downloadArtifact(root, artifact, { fetcher });
  assert.equal(first.fromCache, false);
  assert.deepEqual(await fs.readFile(first.path), bytes);
  const second = await downloadArtifact(root, artifact, { fetcher });
  assert.equal(second.fromCache, true);
  assert.equal(requests, 1);
  await assert.rejects(fs.stat(first.path + '.part'), { code: 'ENOENT' });
});
test('optional SHA-256 is checked for downloads and cache even when SHA-1 is correct', async (t) => {
  const { root, downloadArtifact, verifiedCachedArtifact } = await fixture(t);
  const strong = { ...artifact, sha256: createHash('sha256').update(bytes).digest('hex') };
  const result = await downloadArtifact(root, strong, { fetcher: async () => new Response(bytes) });
  assert.equal(await verifiedCachedArtifact(root, strong), result.path);
  const wrong = { ...strong, sha256: '0'.repeat(64) };
  await assert.rejects(verifiedCachedArtifact(root, wrong), /целостности/);
  await assert.rejects(
    downloadArtifact(root, wrong, { fetcher: async () => new Response(bytes) }),
    /Хеш/,
  );
  assert.deepEqual(await fs.readFile(result.path), bytes);
});
test('Maven Central accepts only exact controlled logging URL, path, size and both hashes', async (t) => {
  const { validateArtifact } = await fixture(t);
  const { LOGGING_LIBRARIES } = await import('../src/main/logging-libraries.ts');
  const pin = LOGGING_LIBRARIES[0];
  assert.deepEqual(validateArtifact(pin), pin);
  for (const changed of [
    { sha1: '0'.repeat(40) },
    { sha256: undefined },
    { sha256: '0'.repeat(64) },
    { size: pin.size + 1 },
    { relativePath: 'libraries/other.jar' },
    { url: pin.url.replace('2.26.1', '2.0-beta9') },
    { url: pin.url + '.sha1' },
  ])
    assert.throws(() => validateArtifact({ ...pin, ...changed }));
});
test('cancelled download keeps its partial bytes and resumes with a verified range', async (t) => {
  const { root, downloadArtifact } = await fixture(t);
  const controller = new AbortController();
  let emitted = false;
  await assert.rejects(
    downloadArtifact(root, artifact, {
      signal: controller.signal,
      fetcher: async () =>
        new Response(
          new ReadableStream({
            pull(stream) {
              if (!emitted) {
                emitted = true;
                stream.enqueue(bytes.subarray(0, 7));
              } else stream.close();
            },
          }),
        ),
      onProgress: (received) => {
        if (received >= 7) controller.abort();
      },
    }),
    { name: 'AbortError' },
  );
  const partial = path.join(root, artifact.relativePath + '.part');
  assert.equal((await fs.stat(partial)).size, 7);
  const resumed = await downloadArtifact(root, artifact, {
    fetcher: async (_url, options) => {
      assert.equal(options.headers.Range, 'bytes=7-');
      return new Response(bytes.subarray(7), {
        status: 206,
        headers: { 'Content-Range': `bytes 7-${bytes.length - 1}/${bytes.length}` },
      });
    },
  });
  assert.deepEqual(await fs.readFile(resumed.path), bytes);
});
test('server ignoring Range restarts the file and incorrect ranges never append', async (t) => {
  const { root, downloadArtifact } = await fixture(t);
  const target = path.join(root, artifact.relativePath);
  await fs.mkdir(path.dirname(target), { recursive: true });
  await fs.writeFile(target + '.part', bytes.subarray(0, 7));
  await assert.rejects(
    downloadArtifact(root, artifact, {
      fetcher: async () =>
        new Response(bytes, { status: 206, headers: { 'Content-Range': 'bytes 0-2/3' } }),
    }),
    /диапазон/,
  );
  assert.equal((await fs.stat(target + '.part')).size, 7);
  await downloadArtifact(root, artifact, { fetcher: async () => new Response(bytes) });
  assert.deepEqual(await fs.readFile(target), bytes);
});
test('bad hash never replaces an existing file and bad partial data is discarded', async (t) => {
  const { root, downloadArtifact } = await fixture(t);
  const target = path.join(root, artifact.relativePath);
  await fs.mkdir(path.dirname(target), { recursive: true });
  const old = Buffer.from('previous file');
  await fs.writeFile(target, old);
  await assert.rejects(
    downloadArtifact(root, artifact, {
      fetcher: async () => new Response(Buffer.alloc(bytes.length)),
    }),
    /Хеш/,
  );
  assert.deepEqual(await fs.readFile(target), old);
  await assert.rejects(fs.stat(target + '.part'), { code: 'ENOENT' });
});
test('oversized, short and unsuccessful responses cannot become completed artifacts', async (t) => {
  const { root, downloadArtifact } = await fixture(t);
  await assert.rejects(
    downloadArtifact(root, artifact, { fetcher: async () => new Response('no', { status: 503 }) }),
    /HTTP 503/,
  );
  await assert.rejects(
    downloadArtifact(root, artifact, {
      fetcher: async () => new Response(bytes, { headers: { 'Content-Length': '999' } }),
    }),
    /Размер/,
  );
  await assert.rejects(
    downloadArtifact(root, artifact, {
      fetcher: async () => new Response(Buffer.alloc(bytes.length + 1)),
    }),
    /превысил/,
  );
  await assert.rejects(
    downloadArtifact(root, artifact, { fetcher: async () => new Response(bytes.subarray(0, 5)) }),
    /оборвалась/,
  );
  await assert.rejects(fs.stat(path.join(root, artifact.relativePath)), { code: 'ENOENT' });
});
test('unsafe paths, hosts, URL credentials and inconsistent hashes are rejected before fetching', async (t) => {
  const { root, downloadArtifact, validateArtifact } = await fixture(t);
  for (const relativePath of [
    '../outside.jar',
    '/outside.jar',
    'C:/outside.jar',
    'libraries/CON.jar',
    'a//file.jar',
    'a/./file.jar',
    'a/file.',
    'a\\file.jar',
  ])
    assert.throws(() => validateArtifact({ ...artifact, relativePath }));
  for (const url of [
    'http://launcher.mojang.com/client.jar',
    'https://example.com/client.jar',
    'https://user:password@launcher.mojang.com/client.jar',
    artifact.url + '?x=1',
    'https://resources.download.minecraft.net/aa/' + sha1,
  ]) {
    await assert.rejects(
      downloadArtifact(
        root,
        { ...artifact, url },
        {
          fetcher: async () => {
            throw new Error('UNSAFE FETCH');
          },
        },
      ),
      (error) => !error.message.includes('UNSAFE FETCH'),
    );
  }
});
test('a symlinked parent cannot direct downloads outside the isolated root', async (t) => {
  const { root, downloadArtifact } = await fixture(t);
  const outside = await fs.mkdtemp(path.join(os.tmpdir(), 'space-outside-'));
  assert.equal(path.dirname(outside), os.tmpdir());
  t.after(() => fs.rm(outside, { recursive: true, force: true }));
  await fs.symlink(
    outside,
    path.join(root, 'versions'),
    process.platform === 'win32' ? 'junction' : 'dir',
  );
  await assert.rejects(
    downloadArtifact(root, artifact, { fetcher: async () => new Response(bytes) }),
    /ссылку/,
  );
  assert.deepEqual(await fs.readdir(outside), []);
});
test('a second concurrent download cannot write to the same artifact', async (t) => {
  const { root, downloadArtifact } = await fixture(t);
  let entered;
  const started = new Promise((resolve) => {
    entered = resolve;
  });
  let release;
  const first = downloadArtifact(root, artifact, {
    fetcher: () =>
      new Promise((resolve) => {
        release = resolve;
        entered();
      }),
  });
  await started;
  await assert.rejects(
    downloadArtifact(root, artifact, { fetcher: async () => new Response(bytes) }),
    /уже загружается/,
  );
  release(new Response(bytes));
  assert.deepEqual(await fs.readFile((await first).path), bytes);
});
