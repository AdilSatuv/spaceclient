import { useEffect, useRef, useState } from 'react';
import {
  ArrowDownToLine,
  Check,
  CircleCheck,
  LoaderCircle,
  LockKeyhole,
  RefreshCw,
  Square,
} from 'lucide-react';
import { installationRunning } from '../shared/installation';
import type { InstallationStatus } from '../shared/installation';
import type { Result } from '../shared/contracts';
import { Button } from './components/ui/button';
import { Badge } from './components/ui/badge';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './components/ui/card';
import { cn } from './lib/utils';

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
  const prepared = current?.phase === 'prepared';
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
    <Card className="installation-card" aria-label="Подготовка файлов игры">
      <CardHeader>
        <div className="installation-heading">
          <span className={cn('section-icon', prepared && 'ready-icon')}>
            {prepared ? <CircleCheck aria-hidden="true" /> : <ArrowDownToLine aria-hidden="true" />}
          </span>
          <div>
            <CardTitle>
              <h2>{prepared ? 'Файлы на месте.' : 'Ещё один шаг к игре.'}</h2>
            </CardTitle>
            <CardDescription>Minecraft 1.8.9 · Fabric Loader · Space Client</CardDescription>
          </div>
          <Badge variant="outline" className={cn('installation-badge', prepared && 'ready-badge')}>
            {prepared ? 'Проверено' : running ? 'Подготовка' : 'Установка'}
          </Badge>
        </div>
      </CardHeader>
      <CardContent>
        <div className="installation-steps" aria-label="Этапы подготовки">
          <span className="completed-step">
            <Check aria-hidden="true" />
            Профиль создан
          </span>
          <Chevron />
          <span className={prepared ? 'completed-step' : 'current-step'}>
            {prepared ? <Check aria-hidden="true" /> : <span className="step-number">2</span>}Файлы
            игры
          </span>
          <Chevron />
          <span>
            <LockKeyhole aria-hidden="true" />
            Microsoft-вход
          </span>
        </div>
        <div
          className={cn('installation-status', current?.phase === 'error' && 'installation-error')}
          role="status"
          aria-live="polite"
          aria-atomic="true"
        >
          {running && <LoaderCircle className="spinner" aria-hidden="true" />}
          {current?.message || 'Проверяем состояние файлов…'}
        </div>
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
        <div className="installation-actions">
          <div className="installation-footnote">
            {current?.spaceVersion ? (
              <>
                <strong>Space {current.spaceVersion}</strong>
                <span>Сборка для разработки</span>
              </>
            ) : (
              <>
                <strong>Около 147 МиБ</strong>
                <span>Отдельная папка профиля</span>
              </>
            )}
          </div>
          {running ? (
            <Button
              variant="outline"
              disabled={acting || current.phase === 'cancelling'}
              onClick={() => act(() => window.space.cancelInstallation(profileId))}
            >
              <Square aria-hidden="true" />
              {current.phase === 'cancelling' ? 'Останавливаем…' : 'Отменить подготовку'}
            </Button>
          ) : (
            <Button
              disabled={acting || dirty || !current}
              onClick={() => act(() => window.space.startInstallation(profileId))}
            >
              {prepared ? <RefreshCw aria-hidden="true" /> : <ArrowDownToLine aria-hidden="true" />}
              {prepared
                ? 'Проверить и восстановить'
                : current?.phase === 'cancelled' || current?.phase === 'error'
                  ? 'Продолжить подготовку'
                  : 'Подготовить файлы'}
            </Button>
          )}
        </div>
        {dirty && <p className="inline-hint">Сначала сохраните или отмените изменения профиля.</p>}
        <p className="launch-disclaimer">
          <LockKeyhole aria-hidden="true" />
          {current?.spaceVersion
            ? 'Запуск станет доступен после Microsoft-входа и проверки владения игрой.'
            : 'Запуск требует Microsoft-входа, проверки владения и готовой сборки Space.'}
        </p>
      </CardContent>
    </Card>
  );
}
function Chevron() {
  return (
    <span className="step-chevron" aria-hidden="true">
      /
    </span>
  );
}
