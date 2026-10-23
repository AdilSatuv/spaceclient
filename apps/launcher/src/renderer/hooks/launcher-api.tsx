import { createContext, useContext } from 'react';
import type { LauncherApi } from '../../shared/contracts.ts';
import type { ReactNode } from 'react';

const ApiContext = createContext<LauncherApi | null>(null);
export function LauncherApiProvider({ api, children }: { api: LauncherApi; children: ReactNode }) {
  return <ApiContext.Provider value={api}>{children}</ApiContext.Provider>;
}
export function useLauncherApi(): LauncherApi {
  const api = useContext(ApiContext);
  if (!api) throw new Error('LauncherApiProvider is required.');
  return api;
}
