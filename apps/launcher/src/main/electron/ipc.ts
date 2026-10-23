import { ipcMain } from 'electron';
import type { BrowserWindow, IpcMainInvokeEvent } from 'electron';
import { channels } from '../../shared/ipc.ts';
import type { CommandHandlers } from '../../shared/ipc.ts';
import { createDispatcher } from '../application/dispatch.ts';
import { trustedSender } from '../security.ts';

export function registerLauncherIpc(
  commands: CommandHandlers,
  getWindow: () => BrowserWindow | null,
): () => void {
  const dispatch = createDispatcher(commands, (event: IpcMainInvokeEvent) => {
    const window = getWindow();
    if (!window || window.isDestroyed()) return false;
    return trustedSender(
      event.sender,
      window.webContents,
      event.senderFrame,
      window.webContents.mainFrame,
      event.senderFrame?.url ?? '',
    );
  });
  const registered: string[] = [];
  const dispose = () => {
    for (const channel of registered.splice(0)) ipcMain.removeHandler(channel);
  };
  try {
    for (const channel of Object.values(channels)) {
      ipcMain.handle(channel, (event, payload) => dispatch(channel, event, payload));
      registered.push(channel);
    }
  } catch (error) {
    dispose();
    throw error;
  }
  return dispose;
}
