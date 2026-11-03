const { test } = require('node:test');
const assert = require('node:assert/strict');
const { EventEmitter } = require('node:events');
const build = {
  channel: 'development',
  modVersion: '0.1.0',
  minecraftVersion: '1.8.9',
  loaderVersion: '0.18.3',
  gameSourceTree: '123456789abc' + 'a'.repeat(28),
  sha256: 'b'.repeat(64),
  size: 1078594,
};
const profile = { id: 'default', name: 'Orbit', memoryMb: 2048, javaPath: null };
const tick = () => new Promise((resolve) => setImmediate(resolve));

test('development identity distinguishes source snapshots sharing 0.1.0 and exposes only public values', async () => {
  const { developmentBuildId } = await import('../src/shared/build-identity.ts');
  const { spaceBuildIdentity } = await import('../src/main/build-identity.ts');
  assert.equal(developmentBuildId(build), 'dev-123456789abc');
  assert.notEqual(
    developmentBuildId(build),
    developmentBuildId({ ...build, gameSourceTree: 'c'.repeat(40) }),
  );
  const dto = spaceBuildIdentity({
    ...build,
    relativePath: 'SECRET',
    log: ['SECRET'],
    token: 'SECRET',
  });
  assert.deepEqual(dto, build);
  dto.sha256 = 'c'.repeat(64);
  assert.equal(build.sha256, 'b'.repeat(64));
});

test('bundled identity is available before preparation, detached and never claims installed preflight', async () => {
  const { InstallationService } = await import('../src/main/installation.ts');
  const value = new InstallationService(
    async () => {
      throw new Error('SECRET');
    },
    {
      space: {
        pin: { schemaVersion: 1, ...build, token: 'SECRET' },
        install: async () => {
          throw new Error('unused');
        },
      },
    },
  );
  const idle = value.status('default');
  assert.equal(idle.phase, 'idle');
  assert.equal(idle.spaceVersion, null);
  assert.deepEqual(idle.bundledBuild, build);
  assert.equal('verifiedBuild' in idle, false);
  assert.doesNotMatch(JSON.stringify(idle), /SECRET/);
  idle.bundledBuild.sha256 = 'c'.repeat(64);
  assert.deepEqual(value.status('default').bundledBuild, build);
  value.start('default');
  await value.settled();
  assert.equal(value.status('default').phase, 'error');
  assert.deepEqual(value.status('default').bundledBuild, build);
  const baseOnly = new InstallationService(async () => '/unused');
  assert.equal(baseOnly.status('default').bundledBuild, null);
});

test('installed identity appears only after full preflight; retries, failures and profiles cannot reuse it', async () => {
  const { LaunchService } = await import('../src/main/launch.ts');
  let resolvePlan;
  let rejectPlan;
  const children = [];
  const value = new LaunchService(
    async () => '/unused',
    { schemaVersion: 1, ...build },
    {
      prepare: () =>
        new Promise((resolve, reject) => {
          resolvePlan = resolve;
          rejectPlan = reject;
        }),
      spawn: () => {
        const child = new EventEmitter();
        children.push(child);
        queueMicrotask(() => child.emit('spawn'));
        return child;
      },
    },
  );
  assert.equal(value.status('default').verifiedBuild, null);
  assert.equal(value.start(profile, 'SpaceTester').verifiedBuild, null);
  await tick();
  assert.equal(value.status('default').phase, 'checking');
  assert.equal(value.status('default').verifiedBuild, null);
  resolvePlan({
    executable: 'fake',
    args: [],
    env: {},
    cwd: '/unused',
    verifiedBuild: { ...build, log: ['SECRET'] },
  });
  await tick();
  const success = value.status('default');
  assert.equal(success.phase, 'running');
  assert.deepEqual(success.verifiedBuild.identity, build);
  assert.ok(Number.isFinite(Date.parse(success.verifiedBuild.checkedAt)));
  assert.doesNotMatch(JSON.stringify(success), /SECRET/);
  assert.equal(value.status('second').verifiedBuild, null);
  success.verifiedBuild.identity.sha256 = 'c'.repeat(64);
  assert.deepEqual(value.status('default').verifiedBuild.identity, build);
  children[0].emit('close', 1, null);
  await value.settled();
  assert.deepEqual(
    value.status('default').verifiedBuild.identity,
    build,
    'a later process failure does not erase the historical byte check',
  );
  assert.equal(
    value.start(profile, 'SpaceTester').verifiedBuild,
    null,
    'retry clears the previous proof before any await',
  );
  await tick();
  rejectPlan(new Error('SECRET'));
  await value.settled();
  assert.equal(value.status('default').phase, 'error');
  assert.equal(value.status('default').verifiedBuild, null);
  assert.doesNotMatch(JSON.stringify(value.status('default')), /SECRET/);
});
