import { contextBridge, ipcRenderer } from 'electron';
import { channels } from './shared/ipc.ts';
import type { LauncherApi, CommandArgs, CommandName, CommandOutput } from './shared/ipc.ts';
import type { Result } from './shared/models.ts';

const invoke = <K extends CommandName>(
  channel: K,
  ...args: CommandArgs<K>
): Promise<Result<CommandOutput<K>>> => ipcRenderer.invoke(channel, ...args);
const api: LauncherApi = {
  getAccount: () => invoke(channels.getAccount),
  registerAccount: (input) => invoke(channels.registerAccount, input),
  loginAccount: (input) => invoke(channels.loginAccount, input),
  logoutAccount: () => invoke(channels.logoutAccount),
  editAccount: (input) => invoke(channels.editAccount, input),
  uploadSkin: (input) => invoke(channels.uploadSkin, input),
  uploadCape: (input) => invoke(channels.uploadCape, input),
  deleteSkin: (revision) => invoke(channels.deleteSkin, revision),
  deleteCape: (revision) => invoke(channels.deleteCape, revision),
  applyAccount: (input) => invoke(channels.applyAccount, input),
  backupProfile: (id) => invoke(channels.backupProfile, id),
  restoreProfile: (input) => invoke(channels.restoreProfile, input),
  exportDiagnostics: (id) => invoke(channels.exportDiagnostics, id),
  saveNickname: (name) => invoke(channels.saveNickname, name),
  getLaunch: (id) => invoke(channels.getLaunch, id),
  startLaunch: (id) => invoke(channels.startLaunch, id),
  getState: () => invoke(channels.getState),
  saveProfile: (edit) => invoke(channels.saveProfile, edit),
  createProfile: () => invoke(channels.createProfile),
  selectProfile: (id) => invoke(channels.selectProfile, id),
  chooseJava: (id) => invoke(channels.chooseJava, id),
  checkJava: (id) => invoke(channels.checkJava, id),
  openInstance: (id) => invoke(channels.openInstance, id),
  getInstallation: (id) => invoke(channels.getInstallation, id),
  startInstallation: (id) => invoke(channels.startInstallation, id),
  cancelInstallation: (id) => invoke(channels.cancelInstallation, id),
};
contextBridge.exposeInMainWorld('space', api);
