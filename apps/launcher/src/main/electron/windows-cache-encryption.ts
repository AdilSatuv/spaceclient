import { app, safeStorage } from 'electron';
import type { CacheEncryption } from '../auth-vault.ts';

// Windows-only until other platforms' key-provider policies are explicitly validated.
export const windowsCacheEncryption: CacheEncryption = {
  available: async () =>
    process.platform === 'win32' &&
    app.isReady() &&
    (await safeStorage.isAsyncEncryptionAvailable()),
  encrypt: (value) => safeStorage.encryptStringAsync(value),
  decrypt: (value) => safeStorage.decryptStringAsync(value),
};
