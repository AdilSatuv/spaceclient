const test = require('node:test');
const assert = require('node:assert/strict');
const { mkdtemp, readFile, rm } = require('node:fs/promises');
const { tmpdir } = require('node:os');
const path = require('node:path');
const { DatabaseSync } = require('node:sqlite');
const { createHash } = require('node:crypto');
const { createApp } = require('../dist/app.js');
const { validatePng } = require('../dist/accounts/png.js');
const { png, chunk, signature } = require('./account-fixtures.cjs');
const { deflateSync } = require('node:zlib');
const password = ' space password with spaces 123 ';
const request = async (base, route, method = 'GET', body, token) => {
  const res = await fetch(base + '/v1/accounts/' + route, {
    method,
    headers: {
      ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
      ...(token ? { Authorization: 'Bearer ' + token } : {}),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  return { status: res.status, body: await res.json() };
};
async function app(options) {
  const app = await createApp(options);
  await app.listen(0, '127.0.0.1');
  return { app, base: await app.getUrl() };
}

test('persistent shared account service: restart, credentials, revisions, media, expiry and revocation', async () => {
  const dir = await mkdtemp(path.join(tmpdir(), 'space-account-'));
  let now = Date.now();
  let server = await app({ accounts: { dataDir: dir, now: () => now, sessionTtlMs: 60000 } });
  try {
    let result = await request(server.base, 'register', 'POST', {
      username: 'SpaceUser',
      nickname: 'SpaceNick',
      password,
    });
    assert.equal(result.status, 201);
    const session = result.body;
    assert.equal(session.profile.revision, 1);
    assert.equal(
      (
        await request(server.base, 'login', 'POST', {
          username: 'spaceuser',
          password: 'wrong password 123456',
        })
      ).body.code,
      'SPACE_INVALID_CREDENTIALS',
    );
    assert.equal(
      (await request(server.base, 'login', 'POST', { username: 'absent_user', password })).body
        .code,
      'SPACE_INVALID_CREDENTIALS',
    );
    assert.equal(
      (
        await request(server.base, 'register', 'POST', {
          username: 'SPACEUSER',
          nickname: 'OtherNick',
          password,
        })
      ).body.code,
      'SPACE_CONFLICT',
    );
    assert.equal(
      (
        await request(server.base, 'register', 'POST', {
          username: 'OtherUser',
          nickname: 'spacenick',
          password,
        })
      ).body.code,
      'SPACE_CONFLICT',
    );
    assert.equal((await request(server.base, 'me')).status, 401);
    result = await request(
      server.base,
      'me',
      'PATCH',
      { nickname: 'NewNick', expectedRevision: 1 },
      session.sessionToken,
    );
    assert.equal(result.body.profile.revision, 2);
    assert.equal(
      (
        await request(
          server.base,
          'me',
          'PATCH',
          { nickname: 'StaleNick', expectedRevision: 1 },
          session.sessionToken,
        )
      ).body.code,
      'SPACE_REVISION_CONFLICT',
    );
    result = await request(
      server.base,
      'me/skin',
      'PUT',
      { pngBase64: png().toString('base64'), model: 'slim', expectedRevision: 2 },
      session.sessionToken,
    );
    assert.equal(result.status, 200);
    const skin = result.body.profile.skin;
    assert.equal(skin.model, 'slim');
    const media = await fetch(server.base + '/v1/accounts/assets/' + skin.sha256);
    const image = Buffer.from(await media.arrayBuffer());
    assert.equal(createHash('sha256').update(image).digest('hex'), skin.sha256);
    assert.equal(image.length, skin.bytes);
    assert.equal(media.headers.get('content-type'), 'image/png');
    result = await request(
      server.base,
      'me/cape',
      'PUT',
      { pngBase64: png(128, 64).toString('base64'), expectedRevision: 3 },
      session.sessionToken,
    );
    assert.equal(result.body.profile.cape.width, 128);
    await server.app.close();
    const db = new DatabaseSync(path.join(dir, 'accounts.sqlite'));
    const account = db.prepare('SELECT * FROM accounts').get();
    const storedSession = db.prepare('SELECT * FROM sessions').get();
    assert.notEqual(account.passwordHash, password);
    assert.equal(
      storedSession.hash,
      createHash('sha256').update(session.sessionToken).digest('hex'),
    );
    assert.ok(!JSON.stringify(account).includes(password));
    db.close();
    server = await app({ accounts: { dataDir: dir, now: () => now, sessionTtlMs: 60000 } });
    result = await request(server.base, 'me', 'GET', undefined, session.sessionToken);
    assert.equal(result.body.profile.nickname, 'NewNick');
    assert.equal(result.body.profile.revision, 4);
    assert.equal(result.body.profile.skin.sha256, skin.sha256);
    const desktop = await request(server.base, 'login', 'POST', {
      username: 'spaceuser',
      password,
    });
    assert.equal(desktop.status, 201);
    result = await request(
      server.base,
      'me/skin',
      'DELETE',
      { expectedRevision: 4 },
      desktop.body.sessionToken,
    );
    assert.equal(result.body.profile.skin, null);
    result = await request(server.base, 'me', 'GET', undefined, session.sessionToken);
    assert.equal(result.body.profile.revision, 5);
    assert.equal(result.body.profile.skin, null);
    assert.equal(
      (await request(server.base, 'logout', 'POST', {}, desktop.body.sessionToken)).body.ok,
      true,
    );
    assert.equal(
      (await request(server.base, 'me', 'GET', undefined, desktop.body.sessionToken)).status,
      401,
    );
    now += 60001;
    assert.equal(
      (await request(server.base, 'me', 'GET', undefined, session.sessionToken)).status,
      401,
    );
  } finally {
    await server.app.close();
    await rm(dir, { recursive: true, force: true });
  }
});

test('independent application stores, strict input, bounded bodies/images and persisted rate limits', async () => {
  const first = await app(),
    second = await app();
  try {
    const session = (
      await request(first.base, 'register', 'POST', {
        username: 'PrivateUser',
        nickname: 'PrivateNick',
        password,
      })
    ).body;
    assert.equal(
      (await request(second.base, 'me', 'GET', undefined, session.sessionToken)).status,
      401,
    );
    assert.equal(
      (await request(second.base, 'login', 'POST', { username: 'PrivateUser', password })).status,
      401,
    );
    for (const body of [
      { username: 'PrivateUser', nickname: 'OtherNick', password, admin: true },
      { username: 'x', nickname: 'Nick', password },
      { username: 'ThirdUser', nickname: 'Nick', password: 'short' },
    ])
      assert.equal(
        (await request(first.base, 'register', 'POST', body)).body.code,
        'SPACE_INVALID_INPUT',
      );
    for (const model of [['slim'], {}, 1]) {
      const invalidModel = await request(
        first.base,
        'me/skin',
        'PUT',
        { pngBase64: png().toString('base64'), model, expectedRevision: 1 },
        session.sessionToken,
      );
      assert.equal(invalidModel.body.code, 'SPACE_INVALID_INPUT');
    }
    for (const bytes of [
      Buffer.from('<svg/>'),
      png(32, 32),
      Buffer.concat([png(), Buffer.from('junk')]),
      png(64, 32),
    ])
      assert.equal(
        (
          await request(
            first.base,
            'me/skin',
            'PUT',
            { pngBase64: bytes.toString('base64'), model: 'slim', expectedRevision: 1 },
            session.sessionToken,
          )
        ).body.code,
        'SPACE_INVALID_IMAGE',
      );
    assert.equal(
      (
        await request(
          first.base,
          'me/cape',
          'PUT',
          { pngBase64: 'A'.repeat(360000), expectedRevision: 1 },
          session.sessionToken,
        )
      ).status,
      400,
    );
    assert.equal(
      (
        await request(
          first.base,
          'me',
          'PATCH',
          { nickname: 'FineName', expectedRevision: 1, admin: true },
          session.sessionToken,
        )
      ).status,
      400,
    );
    assert.equal(
      (await request(first.base, 'me?token=oops', 'GET', undefined, session.sessionToken)).status,
      400,
    );
    const oversized = await fetch(first.base + '/v1/accounts/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ padding: 'x'.repeat(400000) }),
    });
    assert.equal(oversized.status, 413);
    assert.equal((await oversized.json()).code, 'SPACE_INVALID_INPUT');
    const malformed = await fetch(first.base + '/v1/accounts/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: '{"password":"secret"',
    });
    assert.equal(malformed.status, 400);
    assert.ok(!(await malformed.text()).includes('secret'));
    let limited;
    for (let i = 0; i < 16; i++) {
      limited = await request(second.base, 'login', 'POST', { username: 'RateUser', password });
      if (limited.status === 429) break;
    }
    assert.equal(limited.body.code, 'SPACE_RATE_LIMITED');
  } finally {
    await first.app.close();
    await second.app.close();
  }
});

test('PNG full validation checks CRC, chunks, inflate stream, all filters, palette and Adam7 pixels', () => {
  for (const filter of [0, 1, 2, 3, 4])
    assert.equal(validatePng(png(64, 64, { filter }), 'skin', 'classic').width, 64);
  for (const color of [0, 2, 3, 4, 6])
    for (const depth of color === 3 ? [1, 2, 4, 8] : color === 0 ? [1, 2, 4, 8, 16] : [8, 16])
      assert.equal(
        validatePng(
          png(64, 64, {
            color,
            depth,
            palette: color === 3 ? Buffer.from([0, 0, 0]) : undefined,
            interlace: 1,
          }),
          'skin',
          'classic',
        ).height,
        64,
      );
  const valid = png(),
    crc = Buffer.from(valid);
  crc[crc.length - 1] ^= 1;
  const idat = valid.subarray(33, valid.length - 12);
  const header = valid.subarray(8, 33);
  const invalid = [
    crc,
    valid.subarray(0, -1),
    Buffer.concat([signature, header, chunk('IEND', Buffer.alloc(0))]),
    png(64, 64, { filter: 5 }),
    png(64, 64, { compressed: deflateSync(Buffer.alloc(1000000)) }),
    png(64, 64, {
      compressed: Buffer.concat([
        deflateSync(Buffer.alloc(64 * (64 * 4 + 1))),
        Buffer.from('junk'),
      ]),
    }),
    Buffer.concat([
      signature,
      header,
      idat,
      chunk('tEXt', Buffer.from('x')),
      idat,
      chunk('IEND', Buffer.alloc(0)),
    ]),
    png(64, 64, { color: 3, palette: Buffer.from([0, 0, 0]), raw: Buffer.alloc(64 * 65, 255) }),
  ];
  for (const bytes of invalid) assert.throws(() => validatePng(bytes, 'skin', 'classic'));
});
