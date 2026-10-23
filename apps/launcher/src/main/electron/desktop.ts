import { dialog, shell } from 'electron';
import type { BrowserWindow } from 'electron';
import type { DesktopPort } from '../application/ports.ts';
import { UserFacingError } from '../application/user-facing-error.ts';

export function createDesktopPort(getWindow: () => BrowserWindow | null): DesktopPort {
  return {
    chooseJava: async () => {
      const window = getWindow();
      if (!window || window.isDestroyed()) throw new UserFacingError('Окно лаунчера закрыто.');
      const selection = await dialog.showOpenDialog(window, {
        title: 'Выберите Java 21 — java.exe',
        properties: ['openFile'],
        filters: [{ name: 'Java', extensions: ['exe'] }],
      });
      if (selection.canceled) return null;
      if (selection.filePaths.length !== 1)
        throw new UserFacingError('Выберите один файл java.exe.');
      return selection.filePaths[0];
    },
    openDirectory: async (path) => {
      if (await shell.openPath(path))
        throw new UserFacingError('Не удалось открыть папку профиля.');
    },
  };
}
