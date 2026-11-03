import { dialog, shell } from 'electron';
import type { BrowserWindow } from 'electron';
import type { DesktopPort } from '../application/ports.ts';
import { UserFacingError } from '../application/user-facing-error.ts';
import { isAbsolute, dirname, basename, join } from 'node:path';
import { lstat, open, rename, unlink } from 'node:fs/promises';
import { randomUUID } from 'node:crypto';
import { readBounded } from '../bounded-files.ts';

export async function writeDiagnosticFile(path: string, bytes: Uint8Array) {
  if (
    !isAbsolute(path) ||
    path.includes('\0') ||
    path.startsWith('\\\\') ||
    path.startsWith('//') ||
    !basename(path).toLowerCase().endsWith('.json') ||
    /^(con|prn|aux|nul|com[1-9]|lpt[1-9])(?:\.|$)/i.test(basename(path)) ||
    bytes.length > 64 * 1024
  )
    throw new UserFacingError('Выберите локальный JSON-файл для диагностики.');
  let parent = dirname(path);
  while (true) {
    const metadata = await lstat(parent);
    if (!metadata.isDirectory() || metadata.isSymbolicLink())
      throw new UserFacingError('Папка экспорта не может быть ссылкой.');
    const next = dirname(parent);
    if (next === parent) break;
    parent = next;
  }
  await readBounded(path, 1024 * 1024);
  const temporary = join(dirname(path), '.space-diagnostic-' + randomUUID() + '.tmp');
  try {
    const file = await open(temporary, 'wx', 0o600);
    try {
      await file.writeFile(bytes);
      await file.sync();
    } finally {
      await file.close();
    }
    await readBounded(path, 1024 * 1024);
    await rename(temporary, path);
  } finally {
    await unlink(temporary).catch(() => {});
  }
}

export function createDesktopPort(getWindow: () => BrowserWindow | null): DesktopPort {
  return {
    choosePng: async (kind) => {
      const window = getWindow();
      if (!window || window.isDestroyed()) throw new UserFacingError('Окно лаунчера закрыто.');
      const selection = await dialog.showOpenDialog(window, {
        title: kind === 'skin' ? 'Выберите PNG скина' : 'Выберите PNG плаща',
        properties: ['openFile'],
        filters: [{ name: 'PNG', extensions: ['png'] }],
      });
      if (selection.canceled) return null;
      if (selection.filePaths.length !== 1) throw new UserFacingError('Выберите один PNG.');
      return selection.filePaths[0]!;
    },
    exportReport: async (bytes) => {
      const window = getWindow();
      if (!window || window.isDestroyed()) throw new UserFacingError('Окно лаунчера закрыто.');
      const selection = await dialog.showSaveDialog(window, {
        title: 'Экспорт безопасной диагностики',
        defaultPath: 'Space-diagnostics.json',
        filters: [{ name: 'JSON', extensions: ['json'] }],
      });
      if (selection.canceled || !selection.filePath) return false;
      await writeDiagnosticFile(selection.filePath, bytes);
      return true;
    },
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
