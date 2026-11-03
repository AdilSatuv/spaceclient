import { DatabaseSync } from 'node:sqlite';
import { randomBytes, randomUUID, createHash, scrypt, timingSafeEqual } from 'node:crypto';
import { mkdirSync, lstatSync, existsSync } from 'node:fs';
import { resolve, parse, join } from 'node:path';
import { NICKNAME_PATTERN, readSpaceAccountProfile } from '@space/account-contracts';
import type { SpaceAccountProfile } from '@space/account-contracts';
import { credentials, fields, revision, imageInput, badInput } from './input';
import { accountError } from './errors';
import { validatePng } from './png';

export type AccountOptions = { dataDir?: string; now?: () => number; sessionTtlMs?: number };
type Row = {
  id: string;
  username: string;
  nickname: string;
  salt: string;
  passwordHash: string;
  revision: number;
  createdAt: string;
  updatedAt: string;
  skin: string | null;
  cape: string | null;
};
const digest = (text: string | Buffer) => createHash('sha256').update(text).digest('hex');
const unavailable = () =>
  accountError(503, 'SPACE_UNAVAILABLE', 'Сервис аккаунтов временно недоступен. Попробуйте позже.');
const unauthorized = () =>
  accountError(401, 'SPACE_UNAUTHORIZED', 'Войдите в аккаунт Space заново.');
function databasePath(directory: string) {
  const full = resolve(directory),
    root = parse(full).root;
  let current = root;
  for (const segment of full.slice(root.length).split(/[\\/]/).filter(Boolean)) {
    current = join(current, segment);
    if (!existsSync(current)) mkdirSync(current, { mode: 0o700 });
    const info = lstatSync(current);
    if (info.isSymbolicLink() || !info.isDirectory())
      throw new Error('Unsafe account storage directory');
  }
  const filename = join(full, 'accounts.sqlite');
  for (const suffix of ['', '-wal', '-shm'])
    if (existsSync(filename + suffix)) {
      const info = lstatSync(filename + suffix);
      if (info.isSymbolicLink() || !info.isFile()) throw new Error('Unsafe account database');
    }
  return filename;
}
export class AccountService {
  private readonly db: DatabaseSync;
  private readonly now: () => number;
  private readonly ttl: number;
  private deriving = 0;
  constructor(options: AccountOptions = {}) {
    this.now = options.now ?? Date.now;
    this.ttl = options.sessionTtlMs ?? 7 * 24 * 60 * 60 * 1000;
    if (!Number.isSafeInteger(this.ttl) || this.ttl < 1 || this.ttl > 30 * 24 * 60 * 60 * 1000)
      throw new Error('Invalid session lifetime');
    this.db = new DatabaseSync(options.dataDir ? databasePath(options.dataDir) : ':memory:', {
      timeout: 5000,
      allowExtension: false,
    });
    try {
      const version = this.db.prepare('PRAGMA user_version').get() as { user_version: number };
      if (version.user_version > 1) throw new Error('Unsupported account database version');
      this.db.exec(`PRAGMA foreign_keys=ON; PRAGMA journal_mode=WAL; PRAGMA synchronous=FULL;
        CREATE TABLE IF NOT EXISTS accounts(id TEXT PRIMARY KEY,username TEXT NOT NULL,usernameKey TEXT NOT NULL UNIQUE,nickname TEXT NOT NULL,nicknameKey TEXT NOT NULL UNIQUE,salt TEXT NOT NULL,passwordHash TEXT NOT NULL,revision INTEGER NOT NULL,createdAt TEXT NOT NULL,updatedAt TEXT NOT NULL,skin TEXT,cape TEXT);
        CREATE TABLE IF NOT EXISTS sessions(hash TEXT PRIMARY KEY,accountId TEXT NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,expires INTEGER NOT NULL,created INTEGER NOT NULL);
        CREATE INDEX IF NOT EXISTS sessions_account ON sessions(accountId);
        CREATE TABLE IF NOT EXISTS assets(hash TEXT PRIMARY KEY,png BLOB NOT NULL);
        CREATE TABLE IF NOT EXISTS limits(key TEXT PRIMARY KEY,count INTEGER NOT NULL,expires INTEGER NOT NULL);
        PRAGMA user_version=1;`);
    } catch (error) {
      this.db.close();
      throw error;
    }
  }
  onModuleDestroy() {
    if (this.db.isOpen) this.db.close();
  }
  private transaction<T>(operation: () => T): T {
    this.db.exec('BEGIN IMMEDIATE');
    try {
      const result = operation();
      this.db.exec('COMMIT');
      return result;
    } catch (error) {
      this.db.exec('ROLLBACK');
      if (String(error).includes('UNIQUE constraint failed'))
        accountError(409, 'SPACE_CONFLICT', 'Это имя аккаунта или ник уже заняты.');
      throw error;
    }
  }
  spend(key: string, limit: number, windowMs: number) {
    const now = this.now();
    this.db.prepare('DELETE FROM limits WHERE expires<=?').run(now);
    const hash = digest(key),
      row = this.db.prepare('SELECT count FROM limits WHERE key=?').get(hash) as
        { count: number } | undefined;
    if ((row?.count ?? 0) >= limit)
      accountError(
        429,
        'SPACE_RATE_LIMITED',
        'Слишком много запросов. Подождите и попробуйте снова.',
      );
    if (!row && Number(this.db.prepare('SELECT count(*) AS n FROM limits').get()!.n) >= 10000)
      unavailable();
    this.db
      .prepare(
        'INSERT INTO limits(key,count,expires) VALUES(?,1,?) ON CONFLICT(key) DO UPDATE SET count=count+1',
      )
      .run(hash, now + windowMs);
  }
  private async derive(password: string, salt: string) {
    if (this.deriving >= 2) return unavailable();
    this.deriving++;
    try {
      return await new Promise<Buffer>((ok, fail) =>
        scrypt(
          password,
          Buffer.from(salt, 'hex'),
          32,
          { N: 65536, r: 8, p: 1, maxmem: 80 * 1024 * 1024 },
          (error, result) => (error ? fail(error) : ok(result)),
        ),
      );
    } finally {
      this.deriving--;
    }
  }
  private profile(row: Row): SpaceAccountProfile {
    return readSpaceAccountProfile({
      ...row,
      skin: row.skin ? JSON.parse(row.skin) : null,
      cape: row.cape ? JSON.parse(row.cape) : null,
    });
  }
  private row(id: string) {
    const row = this.db.prepare('SELECT * FROM accounts WHERE id=?').get(id) as Row | undefined;
    if (!row) return unauthorized();
    return row;
  }
  private issue(row: Row) {
    const token = randomBytes(32).toString('base64url'),
      now = this.now(),
      expires = now + this.ttl;
    this.db.prepare('DELETE FROM sessions WHERE expires<=?').run(now);
    this.db
      .prepare('INSERT INTO sessions(hash,accountId,expires,created) VALUES(?,?,?,?)')
      .run(digest(token), row.id, expires, now);
    this.db
      .prepare(
        'DELETE FROM sessions WHERE accountId=? AND hash NOT IN (SELECT hash FROM sessions WHERE accountId=? ORDER BY created DESC,rowid DESC LIMIT 10)',
      )
      .run(row.id, row.id);
    return {
      profile: this.profile(row),
      sessionToken: token,
      expiresAt: new Date(expires).toISOString(),
    };
  }
  async register(value: unknown, peer: string) {
    this.spend(`auth-ip:${peer}`, 60, 15 * 60 * 1000);
    this.spend(`register:${peer}`, 10, 15 * 60 * 1000);
    const input = credentials(value, true),
      salt = randomBytes(16).toString('hex');
    const hash = await this.derive(input.password, salt),
      now = new Date(this.now()).toISOString();
    return this.transaction(() => {
      const id = randomUUID();
      this.db
        .prepare(
          'INSERT INTO accounts(id,username,usernameKey,nickname,nicknameKey,salt,passwordHash,revision,createdAt,updatedAt) VALUES(?,?,?,?,?,?,?,1,?,?)',
        )
        .run(
          id,
          input.username,
          input.username.toLowerCase(),
          input.nickname,
          input.nickname.toLowerCase(),
          salt,
          hash.toString('hex'),
          now,
          now,
        );
      return this.issue(this.row(id));
    });
  }
  async login(value: unknown, peer: string) {
    this.spend(`auth-ip:${peer}`, 60, 15 * 60 * 1000);
    const input = credentials(value);
    this.spend(`login:${peer}:${input.username.toLowerCase()}`, 15, 15 * 60 * 1000);
    const row = this.db
      .prepare('SELECT * FROM accounts WHERE usernameKey=?')
      .get(input.username.toLowerCase()) as Row | undefined;
    const hash = await this.derive(input.password, row?.salt ?? '0'.repeat(32));
    if (!timingSafeEqual(hash, Buffer.from(row?.passwordHash ?? '0'.repeat(64), 'hex')) || !row)
      accountError(401, 'SPACE_INVALID_CREDENTIALS', 'Неверное имя аккаунта или пароль.');
    return this.transaction(() => this.issue(row!));
  }
  authenticate(token: unknown) {
    if (typeof token !== 'string' || !/^[A-Za-z0-9_-]{43}$/.test(token)) return unauthorized();
    const session = this.db
      .prepare('SELECT accountId FROM sessions WHERE hash=? AND expires>?')
      .get(digest(token), this.now()) as { accountId: string } | undefined;
    if (!session) return unauthorized();
    return this.row(session.accountId);
  }
  me(token: unknown) {
    return { profile: this.profile(this.authenticate(token)) };
  }
  logout(token: unknown) {
    this.authenticate(token);
    this.db.prepare('DELETE FROM sessions WHERE hash=?').run(digest(token as string));
    return { ok: true };
  }
  private edit(
    token: unknown,
    expected: number,
    patch: Partial<Row>,
    asset?: { hash: string; png: Buffer },
  ) {
    const account = this.authenticate(token);
    return this.transaction(() => {
      const row = this.row(account.id);
      if (row.revision !== expected)
        accountError(
          409,
          'SPACE_REVISION_CONFLICT',
          'Профиль изменился в другом клиенте. Обновите страницу и повторите изменение.',
        );
      if (asset)
        this.db
          .prepare('INSERT OR IGNORE INTO assets(hash,png) VALUES(?,?)')
          .run(asset.hash, asset.png);
      const next = { ...row, ...patch },
        updated = new Date(this.now()).toISOString();
      this.db
        .prepare(
          'UPDATE accounts SET nickname=?,nicknameKey=?,skin=?,cape=?,revision=revision+1,updatedAt=? WHERE id=? AND revision=?',
        )
        .run(
          next.nickname,
          next.nickname.toLowerCase(),
          next.skin,
          next.cape,
          updated,
          row.id,
          expected,
        );
      this.db.exec(
        "DELETE FROM assets WHERE hash NOT IN (SELECT json_extract(skin,'$.sha256') FROM accounts WHERE skin IS NOT NULL UNION SELECT json_extract(cape,'$.sha256') FROM accounts WHERE cape IS NOT NULL)",
      );
      return { profile: this.profile(this.row(row.id)) };
    });
  }
  rename(token: unknown, value: unknown) {
    const input = fields(value, ['nickname', 'expectedRevision']);
    if (typeof input.nickname !== 'string' || !NICKNAME_PATTERN.test(input.nickname))
      return badInput();
    return this.edit(token, revision(input.expectedRevision), { nickname: input.nickname });
  }
  upload(token: unknown, value: unknown, kind: 'skin' | 'cape') {
    this.authenticate(token);
    const input = imageInput(value, kind);
    let decoded: ReturnType<typeof validatePng>;
    try {
      decoded = validatePng(input.bytes, kind, input.model);
    } catch {
      return accountError(
        400,
        'SPACE_INVALID_IMAGE',
        'Нужен корректный PNG допустимого размера и формата.',
      );
    }
    const hash = digest(decoded.png),
      descriptor = {
        sha256: hash,
        bytes: decoded.png.length,
        width: decoded.width,
        height: decoded.height,
        ...(kind === 'skin' ? { model: input.model } : {}),
      };
    return this.edit(
      token,
      input.rev,
      { [kind]: JSON.stringify(descriptor) },
      { hash, png: decoded.png },
    );
  }
  remove(token: unknown, value: unknown, kind: 'skin' | 'cape') {
    const input = fields(value, ['expectedRevision']);
    return this.edit(token, revision(input.expectedRevision), { [kind]: null });
  }
  asset(hash: string) {
    if (!/^[a-f0-9]{64}$/.test(hash)) return badInput();
    const row = this.db.prepare('SELECT png FROM assets WHERE hash=?').get(hash) as
      { png: Uint8Array } | undefined;
    if (!row) accountError(404, 'SPACE_INVALID_IMAGE', 'Изображение не найдено.');
    const bytes = Buffer.from(row!.png);
    if (digest(bytes) !== hash) return unavailable();
    return bytes;
  }
}
