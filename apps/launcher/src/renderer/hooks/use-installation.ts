import { useEffect, useRef, useState } from 'react';
import { installationRunning } from '../../shared/installation';
import type { InstallationStatus } from '../../shared/installation';
import type { Result } from '../../shared/contracts';
import { useLauncherApi } from './launcher-api';
export function useInstallation(profileId: string) {
  const api = useLauncherApi();

  const [status, setStatus] = useState<InstallationStatus | null>(null);
  const [error, setError] = useState('');
  const [acting, setActing] = useState(false);
  const actingRef = useRef(false);
  const currentProfile = useRef(profileId);
  currentProfile.current = profileId;
  const current = status?.profileId === profileId ? status : null;
  const running = !!current && installationRunning(current.phase);
  const prepared = current?.phase === 'prepared';
  useEffect(() => {
    let stopped = false;
    let timer: ReturnType<typeof setTimeout>;
    setError('');
    const poll = async () => {
      try {
        const response = await api.getInstallation(profileId);
        if (stopped) return;
        if (response.ok) setStatus(response.value);
        else setError(response.error);
      } catch {
        if (!stopped) setError('Не удалось получить состояние подготовки файлов.');
      } finally {
        if (!stopped) timer = setTimeout(poll, 650);
      }
    };
    void poll();
    return () => {
      stopped = true;
      clearTimeout(timer);
    };
  }, [api, profileId]);
  const act = async (action: () => Promise<Result<InstallationStatus>>) => {
    if (actingRef.current) return;
    actingRef.current = true;
    setActing(true);
    setError('');
    const requestedProfile = profileId;
    try {
      const response = await action();
      if (currentProfile.current !== requestedProfile) return;
      if (response.ok) setStatus(response.value);
      else setError(response.error);
    } catch {
      if (currentProfile.current === requestedProfile)
        setError('Не удалось связаться с лаунчером.');
    } finally {
      actingRef.current = false;
      setActing(false);
    }
  };

  return {
    current,
    running,
    prepared,
    error,
    acting,
    start: () => act(() => api.startInstallation(profileId)),
    cancel: () => act(() => api.cancelInstallation(profileId)),
  };
}
