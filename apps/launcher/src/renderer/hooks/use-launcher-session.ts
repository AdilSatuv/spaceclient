import { useEffect, useMemo, useSyncExternalStore } from 'react';
import { LauncherSession } from '../model/launcher-session.ts';
import { useLauncherApi } from './launcher-api.tsx';

export function useLauncherSession() {
  const api = useLauncherApi();
  const session = useMemo(() => new LauncherSession(api), [api]);
  const state = useSyncExternalStore(session.subscribe, session.getSnapshot);
  useEffect(() => {
    void session.load();
  }, [session]);
  useEffect(() => {
    let stopped = false;
    let timer: ReturnType<typeof setTimeout>;
    const poll = async () => {
      await session.refreshLaunch();
      if (!stopped) timer = setTimeout(poll, 650);
    };
    void poll();
    return () => {
      stopped = true;
      clearTimeout(timer);
    };
  }, [session, state.profile?.id]);
  // useSyncExternalStore owns unsubscription. Pending main commands can finish after unmount;
  // no React component owns their persistence or cancellation.
  return { ...state, session };
}
