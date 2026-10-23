import { contextBridge, ipcRenderer } from 'electron';
import type { LauncherApi } from './shared/contracts.ts';
const api: LauncherApi = {
  getState: () => ipcRenderer.invoke('state:get'),
  saveProfile: (edit) => ipcRenderer.invoke('profile:save', edit),
  createProfile: () => ipcRenderer.invoke('profile:create'),
  selectProfile: (id) => ipcRenderer.invoke('profile:select', id),
  chooseJava: (id) => ipcRenderer.invoke('java:choose', id),
  checkJava: (id) => ipcRenderer.invoke('java:check', id),
  openInstance: (id) => ipcRenderer.invoke('profile:open', id),
};
contextBridge.exposeInMainWorld('space', api);
