import { useEffect, useRef, useState } from 'react';
import { installationRunning } from '../shared/installation';
import type { InstallationStatus } from '../shared/installation';
import type { Result } from '../shared/contracts';

const mib = (bytes: number) =>
  new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 1 }).format(bytes / 1048576) + ' МиБ';
export function InstallationCard({ profileId, dirty }: { profileId: string; dirty: boolean }) {
  const [status, setStatus] = useState<InstallationStatus | null>(null);
  const [error, setError] = useState('');
  const [acting, setActing] = useState(false);
  const actingRef = useRef(false);
  const currentProfile = useRef(profileId);
  currentProfile.current = profileId;
  const current = status?.profileId === profileId ? status : null;
  const running = !!current && installationRunning(current.phase);
  useEffect(() => {
    let stopped = false;
    let timer: ReturnType<typeof setTimeout>;
    setError('');
    const poll = async () => {
      try {
        const response = await window.space.getInstallation(profileId);
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
  }, [profileId]);
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
  return (
    <section className="panel flight-card installation-card" aria-label="Подготовка файлов игры">
      <span className="eyebrow">ПЕРЕД ПЕРВЫМ ПОЛЁТОМ</span>
      <h2>
        {current?.phase === 'prepared'
          ? current.spaceVersion
            ? 'Space на твоей орбите.'
            : 'База для нового мира.'
          : 'Подготовим твой мир.'}
      </h2>
      <p>
        Minecraft 1.8.9, Fabric Loader и обновлённые библиотеки. Около 147 МиБ, отдельная папка
        профиля.
      </p>
      <div className="installation-status" role="status" aria-live="polite" aria-atomic="true">
        {current?.message || 'Проверяем состояние файлов…'}
      </div>
      {current?.spaceVersion && (
        <p className="inline-hint">Space {current.spaceVersion} · сборка для разработки</p>
      )}
      {!!current?.totalBytes && (
        <div className="installation-progress">
          <progress
            aria-label="Проверенные файлы игры"
            max={current.totalBytes}
            value={current.verifiedBytes}
          />
          <div className="installation-counts">
            <span>
              {current.completedFiles} / {current.totalFiles} файлов
            </span>
            <span>
              {mib(current.verifiedBytes)} / {mib(current.totalBytes)}
            </span>
          </div>
          {running && (
            <p className="inline-hint">
              Получено {mib(current.receivedBytes)} · повторов: {current.retryCount}
            </p>
          )}
        </div>
      )}
      {error && (
        <p className="installation-error" role="alert">
          {error}
        </p>
      )}
      <div className="form-actions">
        {running ? (
          <button
            className="secondary"
            disabled={acting || current.phase === 'cancelling'}
            onClick={() => act(() => window.space.cancelInstallation(profileId))}
          >
            {current.phase === 'cancelling' ? 'Останавливаем…' : 'Отменить подготовку'}
          </button>
        ) : (
          <button
            className="primary"
            disabled={acting || dirty || !current}
            onClick={() => act(() => window.space.startInstallation(profileId))}
          >
            {current?.phase === 'prepared'
              ? 'Проверить и восстановить'
              : current?.phase === 'cancelled' || current?.phase === 'error'
                ? 'Продолжить подготовку'
                : 'Подготовить файлы'}
          </button>
        )}
      </div>
      {dirty && <p className="inline-hint">Сначала сохраните или отмените изменения профиля.</p>}
      <p className="inline-hint">
        {current?.spaceVersion
          ? 'Запуск появится после Microsoft-входа и проверки профиля игры.'
          : 'Запуск появится после подключения Microsoft-входа и сборки Space.'}
      </p>
    </section>
  );
}
