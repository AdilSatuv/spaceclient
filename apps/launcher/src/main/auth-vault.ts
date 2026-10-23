import { lstat, mkdir, open, rename, unlink } from 'node:fs/promises';
import { isAbsolute, join, resolve } from 'node:path';
import { randomUUID } from 'node:crypto';

export const MAX_CACHE_BYTES = 1024 * 1024;
const MAX_CIPHER_BYTES = 2 * MAX_CACHE_BYTES;
export const EMPTY_MSAL_CACHE = JSON.stringify({
  Account: {},
  IdToken: {},
  AccessToken: {},
  RefreshToken: {},
  AppMetadata: {},
});

export interface CacheEncryption {
  available(): Promise<boolean>;
  encrypt(value: string): Promise<Buffer>;
  decrypt(value: Buffer): Promise<{ result: string; shouldReEncrypt: boolean }>;
}

export class AuthVaultError extends Error {
  readonly code: 'unavailable' | 'storage' | 'invalid';
  constructor(code: AuthVaultError['code']) {
    super(
      {
        unavailable: 'Защищённое хранилище Windows сейчас недоступно. Повторите позже.',
        storage:
          'Не удалось открыть или сохранить защищённую сессию. Проверьте доступ к папке лаунчера.',
        invalid:
          'Сохранённую сессию не удалось проверить. Файл сохранён; для повторного входа потребуется явно выйти из аккаунта.',
      }[code],
    );
    this.name = 'AuthVaultError';
    this.code = code;
  }
}

function record(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function safeKeys(value: unknown, depth = 0): void {
  if (depth > 32) throw new AuthVaultError('invalid');
  if (typeof value !== 'object' || value === null) return;
  for (const [key, child] of Object.entries(value)) {
    if (['__proto__', 'constructor', 'prototype'].includes(key))
      throw new AuthVaultError('invalid');
    safeKeys(child, depth + 1);
  }
}

// This bounds and checks the container. MSAL remains responsible for entity semantics.
function validateCache(cache: string): void {
  try {
    if (typeof cache !== 'string' || Buffer.byteLength(cache) > MAX_CACHE_BYTES) throw 0;
    const data: unknown = JSON.parse(cache);
    const categories = ['Account', 'IdToken', 'AccessToken', 'RefreshToken', 'AppMetadata'];
    if (!record(data) || Object.keys(data).length !== categories.length) throw 0;
    for (const name of categories) {
      const entries = data[name];
      if (!record(entries) || Object.keys(entries).length > 1024) throw 0;
      if (Object.entries(entries).some(([key, value]) => key.length > 1024 || !record(value)))
        throw 0;
    }
    safeKeys(data);
  } catch {
    throw new AuthVaultError('invalid');
  }
}

export class EncryptedMsalVault {
  private readonly root: string;
  private readonly directory: string;
  private readonly file: string;
  private readonly clientId: string;
  private readonly encryption: CacheEncryption;
  private queue: Promise<unknown> = Promise.resolve();

  constructor(root: string, clientId: string, encryption: CacheEncryption) {
    if (!isAbsolute(root) || !/^[a-f\d]{8}(?:-[a-f\d]{4}){3}-[a-f\d]{12}$/i.test(clientId)) {
      throw new AuthVaultError('invalid');
    }
    this.root = resolve(root);
    this.directory = join(this.root, 'auth');
    this.file = join(this.directory, 'msal-cache.bin');
    this.clientId = clientId.toLowerCase();
    this.encryption = encryption;
  }

  private serial<T>(action: () => Promise<T>): Promise<T> {
    const result = this.queue.then(action);
    this.queue = result.catch(() => {});
    return result;
  }

  private async paths(create: boolean): Promise<boolean> {
    try {
      const root = await lstat(this.root);
      if (!root.isDirectory() || root.isSymbolicLink()) throw 0;
      let directory;
      try {
        directory = await lstat(this.directory);
      } catch (error) {
        if ((error as NodeJS.ErrnoException).code !== 'ENOENT') throw error;
        if (!create) return false;
        try {
          await mkdir(this.directory, { mode: 0o700 });
        } catch (error) {
          if ((error as NodeJS.ErrnoException).code !== 'EEXIST') throw error;
        }
        directory = await lstat(this.directory);
      }
      if (!directory.isDirectory() || directory.isSymbolicLink()) throw 0;
      try {
        const file = await lstat(this.file);
        if (!file.isFile() || file.isSymbolicLink() || file.nlink !== 1) throw 0;
      } catch (error) {
        if ((error as NodeJS.ErrnoException).code !== 'ENOENT') throw error;
      }
      return true;
    } catch {
      throw new AuthVaultError('storage');
    }
  }

  private async available(): Promise<void> {
    try {
      if (!(await this.encryption.available())) throw 0;
    } catch {
      throw new AuthVaultError('unavailable');
    }
  }

  private async cipher(): Promise<Buffer | null> {
    if (!(await this.paths(false))) return null;
    let handle;
    try {
      handle = await open(this.file, 'r');
    } catch (error) {
      if ((error as NodeJS.ErrnoException).code === 'ENOENT') return null;
      throw new AuthVaultError('storage');
    }
    try {
      const metadata = await handle.stat();
      if (
        !metadata.isFile() ||
        metadata.nlink !== 1 ||
        metadata.size < 1 ||
        metadata.size > MAX_CIPHER_BYTES
      ) {
        throw new AuthVaultError('invalid');
      }
      const bytes = Buffer.alloc(metadata.size + 1);
      let length = 0;
      while (length < bytes.length) {
        const { bytesRead } = await handle.read(bytes, length, bytes.length - length, null);
        if (!bytesRead) break;
        length += bytesRead;
      }
      if (length !== metadata.size) throw new AuthVaultError('invalid');
      return bytes.subarray(0, length);
    } catch (error) {
      throw error instanceof AuthVaultError ? error : new AuthVaultError('storage');
    } finally {
      await handle.close().catch(() => {
        throw new AuthVaultError('storage');
      });
    }
  }

  private async load(): Promise<{ cache: string; rotate: boolean } | null> {
    await this.available();
    const cipher = await this.cipher();
    if (!cipher) return null;
    try {
      const decrypted = await this.encryption.decrypt(cipher);
      if (
        typeof decrypted.result !== 'string' ||
        Buffer.byteLength(decrypted.result) > MAX_CIPHER_BYTES
      )
        throw 0;
      const envelope: unknown = JSON.parse(decrypted.result);
      if (
        !record(envelope) ||
        Object.keys(envelope).length !== 3 ||
        envelope.schemaVersion !== 1 ||
        envelope.clientId !== this.clientId ||
        typeof envelope.cache !== 'string'
      )
        throw 0;
      validateCache(envelope.cache);
      return { cache: envelope.cache, rotate: decrypted.shouldReEncrypt === true };
    } catch (error) {
      if (record(error) && error.isTemporarilyUnavailable === true)
        throw new AuthVaultError('unavailable');
      throw new AuthVaultError('invalid');
    }
  }

  private async persist(cache: string): Promise<void> {
    await this.available();
    let cipher;
    try {
      cipher = await this.encryption.encrypt(
        JSON.stringify({ schemaVersion: 1, clientId: this.clientId, cache }),
      );
      if (!Buffer.isBuffer(cipher) || cipher.length < 1 || cipher.length > MAX_CIPHER_BYTES)
        throw 0;
    } catch {
      throw new AuthVaultError('unavailable');
    }
    await this.paths(true);
    const temporary = join(this.directory, `cache-${randomUUID()}.tmp`);
    try {
      const handle = await open(temporary, 'wx', 0o600);
      try {
        await handle.writeFile(cipher);
        await handle.sync();
      } finally {
        await handle.close();
      }
      await this.paths(false);
      await rename(temporary, this.file);
    } catch {
      throw new AuthVaultError('storage');
    } finally {
      await unlink(temporary).catch(() => {});
    }
  }

  read(): Promise<string | null> {
    return this.serial(async () => {
      const value = await this.load();
      if (value?.rotate) await this.persist(value.cache);
      return value?.cache ?? null;
    });
  }

  write(cache: string): Promise<void> {
    return this.serial(async () => {
      validateCache(cache);
      await this.load(); // Never silently overwrite corrupt or differently bound ciphertext.
      await this.persist(cache);
    });
  }

  // Call only after an explicit logout/reset action. No decryption is needed to forget a broken session.
  clear(): Promise<void> {
    return this.serial(async () => {
      if (!(await this.paths(false))) return;
      try {
        await unlink(this.file);
      } catch (error) {
        if ((error as NodeJS.ErrnoException).code !== 'ENOENT') throw new AuthVaultError('storage');
      }
    });
  }
}
