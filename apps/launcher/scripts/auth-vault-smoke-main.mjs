import { app } from 'electron';
import { readFile, writeFile, stat } from 'node:fs/promises';
import { join } from 'node:path';
import assert from 'node:assert/strict';
import { EncryptedMsalVault } from '../src/main/auth-vault.ts';
import { windowsCacheEncryption } from '../src/main/windows-cache-encryption.ts';

// Only the development runner below starts this entry, with a fresh disposable root.
const root = process.env.SPACE_AUTH_VAULT_SMOKE_ROOT;
if (!root || process.platform !== 'win32') throw new Error('Windows smoke runner required.');
app.setName('Space Client Vault Smoke');
app.setPath('userData', join(root, 'electron'));
const marker = 'FAKE-NONFUNCTIONAL-DPAPI-SMOKE-CACHE';
const cache = JSON.stringify({
  Account: {},
  IdToken: {},
  AccessToken: {},
  AppMetadata: {},
  RefreshToken: { smoke: { secret: marker } },
});
const vault = new EncryptedMsalVault(
  join(root, 'data'),
  '11111111-1111-1111-1111-111111111111',
  windowsCacheEncryption,
);
const file = join(root, 'data/auth/msal-cache.bin');
const report = {
  schemaVersion: 1,
  platform: process.platform,
  electron: process.versions.electron,
  realAccountUsed: false,
};
app
  .whenReady()
  .then(async () => {
    assert.equal(await windowsCacheEncryption.available(), true);
    if (process.argv.includes('--vault-write')) {
      assert.equal(await vault.read(), null);
      await vault.write(cache);
      const bytes = await readFile(file);
      assert.equal(bytes.includes(Buffer.from(marker)), false);
      assert.equal(bytes.includes(Buffer.from('RefreshToken')), false);
      await writeFile(
        join(root, 'write.json'),
        JSON.stringify(
          {
            ...report,
            encryptionAvailable: true,
            plaintextAbsent: true,
            cipherBytes: bytes.length,
          },
          null,
          2,
        ),
      );
    } else if (process.argv.includes('--vault-read')) {
      assert.equal(await vault.read(), cache);
      const bytes = await readFile(file);
      bytes[Math.floor(bytes.length / 2)] ^= 1;
      await writeFile(file, bytes);
      await assert.rejects(vault.read(), { code: 'invalid' });
      await assert.rejects(vault.write(cache), { code: 'invalid' });
      assert.deepEqual(await readFile(file), bytes);
      await vault.clear();
      await assert.rejects(stat(file), { code: 'ENOENT' });
      await writeFile(
        join(root, 'read.json'),
        JSON.stringify(
          {
            ...report,
            restoredAfterProcessRestart: true,
            corruptionRejected: true,
            failedWritePreservedCiphertext: true,
            explicitResetClearedCache: true,
          },
          null,
          2,
        ),
      );
    } else throw new Error('Unknown smoke stage.');
    app.exit(0);
  })
  .catch(() => {
    console.error('Space encrypted cache smoke failed; no cache content is logged.');
    app.exit(1);
  });
