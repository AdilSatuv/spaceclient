import type { ICachePlugin, TokenCacheContext } from '@azure/msal-node';
import { AuthVaultError, EMPTY_MSAL_CACHE, EncryptedMsalVault } from './auth-vault.ts';

// All PCA/cache operations must enter run(), including reads and local logout.
// MSAL callbacks alone do not serialize a complete read-modify-write transaction.
export class MsalCacheAccess {
  readonly plugin: ICachePlugin;
  private readonly vault: EncryptedMsalVault;
  private readonly loaded = new WeakSet<TokenCacheContext>();
  private active = false;
  private queue: Promise<unknown> = Promise.resolve();

  constructor(vault: EncryptedMsalVault) {
    this.vault = vault;
    this.plugin = {
      beforeCacheAccess: async (context) => {
        if (!this.active) throw new AuthVaultError('storage');
        const cache = (await this.vault.read()) ?? EMPTY_MSAL_CACHE;
        try {
          context.tokenCache.deserialize(cache);
        } catch {
          throw new AuthVaultError('invalid');
        }
        this.loaded.add(context);
      },
      afterCacheAccess: async (context) => {
        // MSAL calls this in finally, even if beforeCacheAccess failed.
        if (!this.loaded.delete(context)) return;
        if (!this.active) throw new AuthVaultError('storage');
        if (context.cacheHasChanged) {
          let cache: string;
          try {
            cache = context.tokenCache.serialize();
          } catch {
            throw new AuthVaultError('invalid');
          }
          await this.vault.write(cache);
        }
      },
    };
  }

  run<T>(operation: () => Promise<T>): Promise<T> {
    const result = this.queue.then(async () => {
      this.active = true;
      try {
        return await operation();
      } finally {
        this.active = false;
      }
    });
    this.queue = result.catch(() => {});
    return result;
  }
}
