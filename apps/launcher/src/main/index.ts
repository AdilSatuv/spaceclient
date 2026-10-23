import { app, BrowserWindow, Menu, ipcMain, protocol, dialog, shell } from 'electron';
import { readFile } from 'node:fs/promises';
import { join, resolve } from 'node:path';
import { totalmem } from 'node:os';
import { mkdirSync } from 'node:fs';
import { ProfileStore } from './store.ts';
import { inspectJava } from './java.ts';
import { InstallationService } from './installation.ts';
import { installSpaceBundle } from './game-bundle.ts';
import type { SpaceBundleManifest } from './game-bundle.ts';
import { APP_URL, CSP, assetName, trustedSender } from './security.ts';
import type { Result } from '../shared/contracts.ts';
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
let store: ProfileStore;
let checkingJava = false;
let choosingJava = false;
let installation: InstallationService | null = null;
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
      store = await ProfileStore.open(app.getPath('userData'), Math.floor(totalmem() / 1048576));
      const spacePin = __SPACE_GAME_BUNDLE__;
      installation = new InstallationService(
        async (id) => join(await store.instanceDirectory(id), 'minecraft'),
        {
          space: spacePin
            ? {
                pin: spacePin,
                install: (root, signal) =>
                  installSpaceBundle(
                    root,
                    join(__dirname, 'game', 'space-client.jar'),
                    spacePin,
                    signal,
                  ),
              }
            : null,
        },
      );
      protocol.handle('space', async (request) => {
        const name = request.method === 'GET' ? assetName(request.url) : null;
        if (!name) return new Response('Not found', { status: 404 });
        const type = name.endsWith('.js')
          ? 'text/javascript'
          : name.endsWith('.css')
            ? 'text/css'
            : name.endsWith('.png')
              ? 'image/png'
              : 'text/html';
        try {
          return new Response(await readFile(join(__dirname, 'renderer', name)), {
            headers: {
              'Content-Type': type,
              'Content-Security-Policy': CSP,
              'X-Content-Type-Options': 'nosniff',
            },
          });
        } catch {
          return new Response('Not found', { status: 404 });
        }
      });
      Menu.setApplicationMenu(null);
      window = new BrowserWindow({
        title: 'Space Client',
        width: 1180,
        height: 760,
        minWidth: 1000,
        minHeight: 680,
        show: false,
        backgroundColor: '#0c0e12',
        titleBarStyle: 'hidden',
        titleBarOverlay: { color: '#0f1217', symbolColor: '#f2f4f7', height: 38 },
        webPreferences: {
          preload: join(__dirname, 'preload.cjs'),
          sandbox: true,
          contextIsolation: true,
          nodeIntegration: false,
          webSecurity: true,
          webviewTag: false,
        },
      });
      window.webContents.session.setPermissionRequestHandler((_contents, _permission, callback) =>
        callback(false),
      );
      window.webContents.session.setPermissionCheckHandler(() => false);
      window.webContents.setWindowOpenHandler(() => ({ action: 'deny' }));
      window.webContents.on('will-navigate', (event) => event.preventDefault());
      window.webContents.on('will-redirect', (event) => event.preventDefault());
      window.webContents.on('will-attach-webview', (event) => event.preventDefault());
      const handle = (
        channel: string,
        action: (payload: unknown) => Promise<unknown> | unknown,
      ) => {
        ipcMain.handle(channel, async (event, payload): Promise<Result<unknown>> => {
          if (
            !window ||
            !trustedSender(
              event.sender,
              window.webContents,
              event.senderFrame,
              window.webContents.mainFrame,
              event.senderFrame?.url ?? '',
            )
          )
            return { ok: false, error: 'Действие недоступно.' };
          try {
            return { ok: true, value: await action(payload) };
          } catch (error) {
            return {
              ok: false,
              error: error instanceof Error ? error.message : 'Не удалось выполнить действие.',
            };
          }
        });
      };
      handle('state:get', () => store.snapshot());
      handle('profile:save', (payload) => store.save(payload));
      handle('profile:create', () => store.create());
      handle('profile:select', (payload) => store.select(payload));
      handle('java:choose', async (payload) => {
        if (choosingJava) throw new Error('Выбор Java уже открыт.');
        const profile = store.profile(payload);
        choosingJava = true;
        try {
          const selection = await dialog.showOpenDialog(window!, {
            title: 'Выберите Java 21 — java.exe',
            properties: ['openFile'],
            filters: [{ name: 'Java', extensions: ['exe'] }],
          });
          return selection.canceled
            ? store.snapshot()
            : await store.setJava(profile.id, selection.filePaths[0]);
        } finally {
          choosingJava = false;
        }
      });
      handle('java:check', async (payload) => {
        if (checkingJava) throw new Error('Проверка Java уже выполняется.');
        const profile = store.profile(payload);
        checkingJava = true;
        try {
          return await inspectJava(profile.javaPath);
        } finally {
          checkingJava = false;
        }
      });
      handle('profile:open', async (payload) => {
        const error = await shell.openPath(await store.instanceDirectory(payload));
        if (error) throw new Error('Не удалось открыть папку профиля.');
        return null;
      });
      handle('installation:get', (payload) => installation!.status(store.profile(payload).id));
      handle('installation:start', (payload) => installation!.start(store.profile(payload).id));
      handle('installation:cancel', (payload) => installation!.cancel(store.profile(payload).id));
      window.once('ready-to-show', () => window?.show());
      window.on('closed', () => {
        window = null;
      });
      await window.loadURL(APP_URL);
    })
    .catch(() => {
      dialog.showErrorBox(
        'Space Client',
        'Не удалось открыть лаунчер. Проверьте доступ к папке настроек. Исходные файлы не удалены.',
      );
      app.quit();
    });
}
app.on('window-all-closed', () => app.quit());
app.on('before-quit', (event) => {
  if (!installation?.running) return;
  event.preventDefault();
  if (!shuttingDown) {
    shuttingDown = true;
    void installation.shutdown().finally(() => app.quit());
  }
});
