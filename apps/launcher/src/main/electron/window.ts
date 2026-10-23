import { BrowserWindow } from 'electron';

export function createLauncherWindow(preload: string): BrowserWindow {
  const window = new BrowserWindow({
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
      preload,
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
  window.once('ready-to-show', () => {
    if (!window.isDestroyed()) window.show();
  });
  return window;
}
