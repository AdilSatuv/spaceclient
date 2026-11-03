import { readSpaceAccountSession } from '@space/account-contracts';
import type { SpaceAccountSession } from '@space/account-contracts';
import type { CacheEncryption } from '../auth-vault.ts';
import { atomicFile, readBounded } from '../bounded-files.ts';
import { installationTarget } from '../downloads.ts';
import { UserFacingError } from '../application/user-facing-error.ts';

/** Separate from MSAL. Tokens exist only here/in main HTTP; disk always uses OS encryption. */
export class SpaceSessionVault {
  private readonly root: string;
  private readonly origin: string;
  private readonly encryption: CacheEncryption;
  constructor(root: string, origin: string, encryption: CacheEncryption) {
    this.root = root;
    this.origin = origin;
    this.encryption = encryption;
  }
  private async available() {
    if (!(await this.encryption.available().catch(() => false)))
      throw new UserFacingError(
        'Защищённое хранилище Windows недоступно. Вход Space не сохранён; повторите позже.',
      );
  }
  async load(): Promise<SpaceAccountSession | null> {
    const bytes = await readBounded(
      await installationTarget(this.root, 'auth/space-session.bin'),
      64 * 1024,
    );
    if (!bytes) return null;
    await this.available();
    try {
      const plain = await this.encryption.decrypt(bytes);
      if (Buffer.byteLength(plain.result) > 32 * 1024) throw 0;
      const value = JSON.parse(plain.result);
      if (
        value.schemaVersion !== 1 ||
        value.origin !== this.origin ||
        Object.keys(value).length !== 3
      )
        throw 0;
      const session = readSpaceAccountSession(value.session);
      if (Date.parse(session.expiresAt) <= Date.now()) {
        await this.clear();
        return null;
      }
      if (plain.shouldReEncrypt) await this.save(session);
      return session;
    } catch {
      throw new UserFacingError(
        'Сохранённая сессия Space недоступна. Выйдите из неё и войдите повторно.',
      );
    }
  }
  async save(session: SpaceAccountSession) {
    await this.available();
    const value = readSpaceAccountSession(session);
    if (Date.parse(value.expiresAt) <= Date.now())
      throw new UserFacingError('Сессия Space уже истекла. Войдите снова.');
    let bytes: Buffer;
    try {
      bytes = await this.encryption.encrypt(
        JSON.stringify({ schemaVersion: 1, origin: this.origin, session: value }),
      );
      if (!Buffer.isBuffer(bytes) || !bytes.length || bytes.length > 64 * 1024) throw 0;
    } catch {
      throw new UserFacingError(
        'Не удалось зашифровать сессию Space. Незащищённое сохранение запрещено.',
      );
    }
    await atomicFile(this.root, 'auth/space-session.bin', bytes);
  }
  async clear() {
    await atomicFile(this.root, 'auth/space-session.bin', null);
  }
}
