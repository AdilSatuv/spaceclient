const test = require('node:test');
const assert = require('node:assert/strict');
const { readSpaceAccountProfile, readSpaceAccountSession } = require('@space/account-contracts');
const image = { sha256: 'a'.repeat(64), bytes: 512, width: 64, height: 64, model: 'slim' };
const profile = () => ({
  id: '00000000-0000-4000-8000-000000000001',
  username: 'SpaceUser',
  nickname: 'SpaceNick',
  revision: 1,
  createdAt: '2026-11-03T00:00:00.000Z',
  updatedAt: '2026-11-03T00:00:00.000Z',
  skin: { ...image },
  cape: { ...image, width: 128, height: 64 },
});
test('public account profiles copy only allowed fields, including nested image descriptors', () => {
  const source = { ...profile(), passwordHash: 'secret', salt: 'secret', sessionToken: 'secret' };
  source.skin.url = 'file:///secret';
  const value = readSpaceAccountProfile(source);
  assert.deepEqual(
    Object.keys(value).sort(),
    ['id', 'username', 'nickname', 'revision', 'createdAt', 'updatedAt', 'skin', 'cape'].sort(),
  );
  assert.deepEqual(
    Object.keys(value.skin).sort(),
    ['sha256', 'bytes', 'width', 'height', 'model'].sort(),
  );
  assert.deepEqual(Object.keys(value.cape).sort(), ['sha256', 'bytes', 'width', 'height'].sort());
  source.skin.sha256 = 'b'.repeat(64);
  assert.equal(value.skin.sha256, 'a'.repeat(64));
  assert.equal(JSON.stringify(value).includes('secret'), false);
});
test('account response validation rejects malformed identities, revisions and image metadata', () => {
  const invalid = [
    (p) => (p.id = '../../escape'),
    (p) => (p.username = 'x'),
    (p) => (p.nickname = 'bad\\nname'),
    (p) => (p.revision = 0),
    (p) => (p.revision = Number.MAX_SAFE_INTEGER + 1),
    (p) => (p.createdAt = 'invalid'),
    (p) => (p.skin.sha256 = '../' + 'a'.repeat(61)),
    (p) => (p.skin.bytes = 256 * 1024 + 1),
    (p) => (p.skin.width = 4096),
    (p) => (p.skin.model = ['slim']),
    (p) => (p.skin.model = { toString: () => 'classic' }),
    (p) => (p.cape.height = 128),
    (p) => (p.skin = undefined),
  ];
  for (const mutate of invalid) {
    const value = profile();
    mutate(value);
    assert.throws(() => readSpaceAccountProfile(value));
  }
});
test('supported skin models and cape dimensions survive the shared contract', () => {
  for (const width of [64, 128, 256]) {
    const value = profile();
    value.cape = { ...image, width, height: width / 2 };
    assert.equal(readSpaceAccountProfile(value).cape.width, width);
  }
  const legacy = profile();
  legacy.skin.height = 32;
  legacy.skin.model = 'classic';
  assert.equal(readSpaceAccountProfile(legacy).skin.height, 32);
  legacy.skin.model = 'slim';
  assert.throws(() => readSpaceAccountProfile(legacy));
  const empty = profile();
  empty.skin = empty.cape = null;
  assert.equal(readSpaceAccountProfile(empty).skin, null);
});
test('server session parsing validates and isolates its token from the public profile', () => {
  const source = {
    profile: profile(),
    sessionToken: 'a'.repeat(43),
    expiresAt: '2026-11-10T00:00:00.000Z',
    password: 'secret',
  };
  const value = readSpaceAccountSession(source);
  assert.deepEqual(Object.keys(value).sort(), ['profile', 'sessionToken', 'expiresAt'].sort());
  assert.equal('sessionToken' in value.profile, false);
  for (const sessionToken of ['', 'a'.repeat(42), 'a'.repeat(257), '../token', null])
    assert.throws(() => readSpaceAccountSession({ ...source, sessionToken }));
});
