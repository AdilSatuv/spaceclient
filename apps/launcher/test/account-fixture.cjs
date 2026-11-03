const { deflateSync, crc32 } = require('node:zlib');
const { createHash, randomBytes, createCipheriv, createDecipheriv } = require('node:crypto');
function png(width = 64, height = 64, filter = 0) {
  const chunk = (name, bytes) => {
    const out = Buffer.alloc(bytes.length + 12);
    out.writeUInt32BE(bytes.length);
    out.write(name, 4);
    bytes.copy(out, 8);
    out.writeUInt32BE(crc32(out.subarray(4, bytes.length + 8)), bytes.length + 8);
    return out;
  };
  const header = Buffer.alloc(13);
  header.writeUInt32BE(width);
  header.writeUInt32BE(height, 4);
  header[8] = 8;
  header[9] = 6;
  const pixels = Buffer.alloc((width * 4 + 1) * height, 180);
  for (let y = 0; y < height; y++) pixels[y * (width * 4 + 1)] = filter;
  return Buffer.concat([
    Buffer.from('89504e470d0a1a0a', 'hex'),
    chunk('IHDR', header),
    chunk('IDAT', deflateSync(pixels)),
    chunk('IEND', Buffer.alloc(0)),
  ]);
}
function encryption() {
  const key = randomBytes(32);
  return {
    available: async () => true,
    encrypt: async (value) => {
      const iv = randomBytes(12),
        cipher = createCipheriv('aes-256-gcm', key, iv);
      const bytes = Buffer.concat([cipher.update(value, 'utf8'), cipher.final()]);
      return Buffer.concat([iv, cipher.getAuthTag(), bytes]);
    },
    decrypt: async (bytes) => {
      const cipher = createDecipheriv('aes-256-gcm', key, bytes.subarray(0, 12));
      cipher.setAuthTag(bytes.subarray(12, 28));
      return {
        result: Buffer.concat([cipher.update(bytes.subarray(28)), cipher.final()]).toString('utf8'),
        shouldReEncrypt: false,
      };
    },
  };
}
const profile = () => ({
  id: '12345678-1234-1234-1234-123456789abc',
  username: 'FixtureSpace',
  nickname: 'SpacePlayer',
  revision: 1,
  createdAt: '2026-11-03T00:00:00Z',
  updatedAt: '2026-11-03T00:00:00Z',
  skin: null,
  cape: null,
});
function apiFixture() {
  let current = profile(),
    active = null,
    sequence = 0,
    offline = false;
  const assets = new Map(),
    calls = [];
  const json = (value, status = 200) =>
    new Response(JSON.stringify(value), {
      status,
      headers: { 'content-type': 'application/json' },
    });
  return {
    setOffline(value) {
      offline = value;
    },
    calls,
    assets,
    get profile() {
      return current;
    },
    revoke() {
      active = null;
    },
    fetcher: async (url, options = {}) => {
      if (offline) throw new Error('SECRET connection refused');
      const path = new URL(url).pathname,
        body = options.body ? JSON.parse(options.body) : undefined;
      calls.push({ path, method: options.method, body, token: options.headers?.Authorization });
      if (path.endsWith('/register') || path.endsWith('/login')) {
        active = 's'.repeat(43) + ++sequence;
        return json({ profile: current, sessionToken: active, expiresAt: '2099-01-01T00:00:00Z' });
      }
      if (path.includes('/assets/')) {
        const bytes = assets.get(path.split('/').at(-1));
        return new Response(bytes ?? '', {
          status: bytes ? 200 : 404,
          headers: { 'content-type': 'image/png' },
        });
      }
      if (options.headers?.Authorization !== 'Bearer ' + active || !active)
        return json({ code: 'SPACE_UNAUTHORIZED', message: 'SECRET raw error' }, 401);
      if (path.endsWith('/logout')) {
        active = null;
        return json({ ok: true });
      }
      if (options.method === 'GET')
        return json({ profile: { ...current, passwordHash: 'SECRET' } });
      if (body.expectedRevision !== current.revision)
        return json({ code: 'SPACE_REVISION_CONFLICT', message: 'SECRET' }, 409);
      if (options.method === 'PATCH')
        current = { ...current, nickname: body.nickname, revision: current.revision + 1 };
      else {
        const kind = path.split('/').at(-1);
        if (options.method === 'DELETE')
          current = { ...current, [kind]: null, revision: current.revision + 1 };
        else {
          const bytes = Buffer.from(body.pngBase64, 'base64'),
            sha256 = createHash('sha256').update(bytes).digest('hex');
          assets.set(sha256, bytes);
          current = {
            ...current,
            [kind]: {
              sha256,
              bytes: bytes.length,
              width: bytes.readUInt32BE(16),
              height: bytes.readUInt32BE(20),
              ...(kind === 'skin' ? { model: body.model } : {}),
            },
            revision: current.revision + 1,
          };
        }
      }
      return json({ profile: current });
    },
  };
}
module.exports = { png, encryption, profile, apiFixture };
