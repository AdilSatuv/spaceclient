import { createRoot } from 'react-dom/client';
import type { LauncherApi } from '../shared/contracts.ts';
import { App } from './app';
import { LauncherApiProvider } from './hooks/launcher-api';

declare global {
  interface Window {
    space: LauncherApi;
  }
}
createRoot(document.getElementById('root')!).render(
  <LauncherApiProvider api={window.space}>
    <App />
  </LauncherApiProvider>,
);
