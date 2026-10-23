const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const os = require('node:os');
const hash = 'a'.repeat(40);
const hash2 = 'b'.repeat(40);
function library(name, classifier) {
  const [group, artifact, version] = name.split(':');
  const filePath = `${group.replaceAll('.', '/')}/${artifact}/${version}/${artifact}-${version}${classifier ? '-' + classifier : ''}.jar`;
  const file = {
    path: filePath,
    url: `https://libraries.minecraft.net/${filePath}`,
    sha1: hash,
    size: 10,
  };
  return {
    name,
    downloads: classifier ? { classifiers: { [classifier]: file } } : { artifact: file },
    ...(classifier
      ? {
          natives: { windows: classifier.replace('-64', '-${arch}') },
          extract: { exclude: ['META-INF/'] },
        }
      : {}),
  };
}
async function fixture() {
  const module = await import('../src/main/version-plan.ts');
  return {
    ...module,
    metadata: {
      id: '1.8.9',
      type: 'release',
      assets: '1.8',
      mainClass: 'net.minecraft.client.main.Main',
      assetIndex: { ...module.ASSET_INDEX, id: '1.8', totalSize: 10 },
      downloads: {
        client: {
          url: `https://launcher.mojang.com/v1/objects/${hash}/client.jar`,
          sha1: hash,
          size: 10,
        },
      },
      libraries: [library('example:core:1.0'), library('example:native:1.0', 'natives-windows-64')],
      // Executable remote arguments are deliberately ignored by the plan builder.
      minecraftArguments: 'malicious remote argument',
    },
    index: {
      objects: {
        'sound/a.ogg': { hash: hash2, size: 5 },
        'sound/alias.ogg': { hash: hash2, size: 5 },
      },
    },
  };
}
test('Windows rules use the last matching rule and reject unknown conditions', async () => {
  const { allowedOnWindows } = await fixture();
  assert.equal(allowedOnWindows(undefined), true);
  assert.equal(allowedOnWindows([]), false);
  assert.equal(
    allowedOnWindows([{ action: 'allow' }, { action: 'disallow', os: { name: 'osx' } }]),
    true,
  );
  assert.equal(
    allowedOnWindows([{ action: 'allow' }, { action: 'disallow', os: { name: 'windows' } }]),
    false,
  );
  assert.equal(allowedOnWindows([{ action: 'allow', os: { name: 'linux' } }]), false);
  for (const input of [
    [{ action: 'allow', features: {} }],
    [{ action: 'allow', os: { name: 'windows', arch: 'x86' } }],
    [{ action: 'maybe' }],
  ])
    assert.throws(() => allowedOnWindows(input));
});
test('plan deduplicates hashed assets, selects Windows x64 natives and never becomes launch-ready', async () => {
  const { metadata, index, buildVersionPlan } = await fixture();
  const mac = library('example:mac:1.0');
  mac.rules = [{ action: 'allow', os: { name: 'osx' } }];
  metadata.libraries.push(mac, library('org.apache.logging.log4j:log4j-core:2.0-beta9'));
  const plan = buildVersionPlan(metadata, index);
  assert.equal(plan.assetCount, 2);
  assert.equal(
    plan.files.filter((file) => file.relativePath.startsWith('assets/objects/')).length,
    1,
  );
  assert.equal(plan.natives.length, 2);
  assert.match(plan.natives[0].archive, /natives-windows-64.jar$/);
  assert.deepEqual(plan.natives[0].exclude, ['META-INF/']);
  assert.ok(!plan.files.some((file) => /2\.0-beta9|example\/mac/.test(file.relativePath)));
  const logging = plan.files.filter((file) =>
    file.url.startsWith('https://repo.maven.apache.org/'),
  );
  assert.equal(logging.length, 2);
  assert.ok(
    logging.every(
      (file) => /\/2\.26\.1\//.test(file.relativePath) && /^[a-f0-9]{64}$/.test(file.sha256),
    ),
  );
  assert.equal(plan.loggingVersion, '2.26.1');
  assert.equal(plan.loaderVersion, '0.18.3');
  assert.equal(plan.mainClass, 'net.fabricmc.loader.impl.launch.knot.KnotClient');
  assert.ok(plan.natives.some((native) => native.archive.includes('2.9.4+legacyfabric.17')));
  assert.ok(!plan.classpath.some((path) => path.includes('-natives-windows')));
  assert.deepEqual(plan.excludedLibraries, ['org.apache.logging.log4j:log4j-core:2.0-beta9']);
  assert.equal(plan.classpath.at(-1), 'versions/1.8.9/1.8.9.jar');
  assert.equal(plan.launchReady, false);
  assert.equal('arguments' in plan, false);
  assert.equal(
    plan.totalBytes,
    plan.files.reduce((sum, file) => sum + file.size, 0),
  );
});
test('unsupported versions, modified index pins and legacy asset layouts fail closed', async () => {
  for (const mutate of [
    (m) => {
      m.id = '1.9';
    },
    (m) => {
      m.inheritsFrom = 'other';
    },
    (m) => {
      m.mainClass = 'arbitrary.Main';
    },
    (m) => {
      m.assetIndex.sha1 = hash;
    },
    (_m, i) => {
      i.virtual = true;
    },
    (_m, i) => {
      i.map_to_resources = true;
    },
    (m) => {
      m.assetIndex.totalSize = 999;
    },
  ]) {
    const { metadata, index, buildVersionPlan } = await fixture();
    mutate(metadata, index);
    assert.throws(() => buildVersionPlan(metadata, index));
  }
});
test('Legacy Fabric LWJGL override replaces the original jars and native archive together', async () => {
  const { metadata, index, buildVersionPlan } = await fixture();
  metadata.libraries.push(
    library('org.lwjgl.lwjgl:lwjgl:2.9.4-nightly-20150209'),
    library('org.lwjgl.lwjgl:lwjgl_util:2.9.4-nightly-20150209'),
    library('org.lwjgl.lwjgl:lwjgl-platform:2.9.4-nightly-20150209', 'natives-windows'),
  );
  const plan = buildVersionPlan(metadata, index);
  assert.ok(!plan.files.some((file) => file.relativePath.includes('20150209')));
  assert.equal(plan.classpath.filter((path) => path.includes('2.9.4+legacyfabric.17')).length, 2);
  assert.equal(
    plan.natives.filter((file) => file.archive.includes('2.9.4+legacyfabric.17')).length,
    1,
  );
  assert.ok(plan.natives.some((file) => file.archive.includes('example/native')));
});
test('library coordinate/path/source, classifier and extraction inconsistencies are rejected', async () => {
  for (const mutate of [
    (m) => {
      m.libraries[0].downloads.artifact.path = '../escape.jar';
    },
    (m) => {
      m.libraries[0].downloads.artifact.url = 'https://libraries.minecraft.net/wrong.jar';
    },
    (m) => {
      m.libraries.push(m.libraries[0]);
    },
    (m) => {
      m.libraries[1].natives.windows = 'natives-linux';
    },
    (m) => {
      m.libraries[1].extract.exclude = ['../'];
    },
    (m) => {
      delete m.libraries[1].downloads.classifiers['natives-windows-64'];
    },
  ]) {
    const { metadata, index, buildVersionPlan } = await fixture();
    mutate(metadata);
    assert.throws(() => buildVersionPlan(metadata, index));
  }
});
test('asset hashes and sizes are checked; logical names cannot become filesystem paths', async () => {
  const { metadata, index, buildVersionPlan } = await fixture();
  index.objects['../../outside'] = index.objects['sound/a.ogg'];
  metadata.assetIndex.totalSize = 15;
  const plan = buildVersionPlan(metadata, index);
  assert.ok(plan.files.every((file) => !file.relativePath.includes('outside')));
  index.objects['sound/a.ogg'] = { hash: '../bad', size: 5 };
  assert.throws(() => buildVersionPlan(metadata, index));
  index.objects['sound/a.ogg'] = { hash: hash2, size: 6 };
  metadata.assetIndex.totalSize = 16;
  assert.throws(() => buildVersionPlan(metadata, index), /конфликт/);
});
test('preparation rejects bad metadata integrity before requesting the asset index', async (t) => {
  const { prepareVersionPlan } = await import('../src/main/prepare-plan.ts');
  const { VERSION_METADATA } = await fixture();
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'space-plan-'));
  assert.equal(path.dirname(root), os.tmpdir());
  t.after(() => fs.rm(root, { recursive: true, force: true }));
  let calls = 0;
  await assert.rejects(
    prepareVersionPlan(root, {
      fetcher: async () => {
        calls++;
        return new Response(Buffer.alloc(VERSION_METADATA.size));
      },
    }),
    /Хеш/,
  );
  assert.equal(calls, 1);
  const controller = new AbortController();
  controller.abort();
  await assert.rejects(
    prepareVersionPlan(root, {
      signal: controller.signal,
      fetcher: async () => {
        throw new Error('must not fetch');
      },
    }),
    { name: 'AbortError' },
  );
});
