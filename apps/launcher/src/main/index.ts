import { app, BrowserWindow, Menu, protocol, dialog } from 'electron';
import { join, resolve } from 'node:path';
import { totalmem } from 'node:os';
import { mkdirSync } from 'node:fs';
import { ProfileStore } from './store.ts';
import { inspectJava } from './java.ts';
import { InstallationService } from './installation.ts';
import { LaunchService } from './launch.ts';
import { installSpaceBundle } from './game-bundle.ts';
import type { SpaceBundleManifest } from './game-bundle.ts';
import { APP_URL } from './security.ts';
import { LauncherApplication } from './application/launcher.ts';
import { createLauncherWindow } from './electron/window.ts';
import { createDesktopPort } from './electron/desktop.ts';
import { registerLauncherIpc } from './electron/ipc.ts';
import { createAssetHandler } from './electron/assets.ts';
import { AccountHttp, DEFAULT_ACCOUNT_API } from './account/http.ts';
import { SpaceSessionVault } from './account/session.ts';
import { SpaceAccountService } from './account/service.ts';
import { windowsCacheEncryption } from './electron/windows-cache-encryption.ts';
import { ProfileRecovery } from './profile-recovery.ts';

declare const __SPACE_GAME_BUNDLE__: SpaceBundleManifest | null;

app.setName('Space Client');
if (!app.isPackaged && process.env.SPACE_LAUNCHER_DATA) {
  const dataRoot = resolve(process.env.SPACE_LAUNCHER_DATA);
  mkdirSync(dataRoot, { recursive: true });
  app.setPath('userData', dataRoot);
}
protocol.registerSchemesAsPrivileged([
  { scheme: 'space', privileges: { standard: true, secure: true, supportFetchAPI: true } },
]);
let window: BrowserWindow | null = null;
let application: LauncherApplication | null = null;
let disposeIpc: (() => void) | undefined;
let shuttingDown = false;
if (!app.requestSingleInstanceLock()) app.quit();
else {
  app.on('second-instance', () => {
    if (window?.isMinimized()) window.restore();
    window?.focus();
  });
  app
    .whenReady()
    .then(async () => {
      const profiles = await ProfileStore.open(
        app.getPath('userData'),
        Math.floor(totalmem() / 1048576),
      );
      const pin = __SPACE_GAME_BUNDLE__;
      const installation = new InstallationService(
        async (id) => join(await profiles.instanceDirectory(id), 'minecraft'),
        {
          space: pin
            ? {
                pin,
                install: (root, signal) =>
                  installSpaceBundle(
                    root,
                    join(__dirname, 'game', 'space-client.jar'),
                    pin,
                    signal,
                  ),
              }
            : null,
        },
      );
      const desktop = createDesktopPort(() => window);
      const instance = async (id: string) =>
        join(await profiles.instanceDirectory(id), 'minecraft');
      let account: SpaceAccountService | undefined;
      try {
        const http = new AccountHttp(process.env.SPACE_ACCOUNT_API_URL || DEFAULT_ACCOUNT_API);
        account = new SpaceAccountService(
          http,
          new SpaceSessionVault(profiles.root, http.base, windowsCacheEncryption),
          {
            rootForProfile: instance,
            localNickname: () => profiles.snapshot().localNickname,
            saveNickname: (name) => profiles.saveNickname(name),
            choosePng: (kind) => desktop.choosePng!(kind),
          },
        );
      } catch {
        /* Invalid operator configuration must not disable the guest launcher. */
      }
      const launch = new LaunchService(instance, pin, {
        beforeLaunch: async (id, root, name, signal) => {
          await account?.beforeLaunch(id, root, name, signal);
        },
      });
      const recovery = new ProfileRecovery(profiles.root, {
        profiles,
        installation: (id) => installation.status(id),
        launch: (id) => launch.status(id),
        build: pin,
        exportReport: (bytes) => desktop.exportReport!(bytes),
      });
      application = new LauncherApplication({
        account,
        recovery,
        profiles,
        installation,
        launch,
        inspectJava,
        desktop,
      });
      protocol.handle('space', createAssetHandler(join(__dirname, 'renderer')));
      Menu.setApplicationMenu(null);
      window = createLauncherWindow(
        join(__dirname, 'preload.cjs'),
        app.isPackaged || process.env.SPACE_LAUNCHER_TEST_HIDDEN !== '1',
      );
      disposeIpc = registerLauncherIpc(application.commands, () => window);
      window.on('closed', () => {
        window = null;
        disposeIpc?.();
        disposeIpc = undefined;
      });
      await window.loadURL(APP_URL);
    })
    .catch(() => {
      disposeIpc?.();
      dialog.showErrorBox(
        'Space Client',
        'Не удалось открыть лаунчер. Проверьте доступ к папке настроек. Исходные файлы не удалены.',
      );
      app.quit();
    });
}
app.on('window-all-closed', () => app.quit());
app.on('before-quit', (event) => {
  if (!application?.running) return;
  event.preventDefault();
  if (!shuttingDown) {
    shuttingDown = true;
    void application.shutdown().finally(() => app.quit());
  }
});
