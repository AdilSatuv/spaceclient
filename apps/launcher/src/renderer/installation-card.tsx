import type { useInstallation } from './hooks/use-installation';
import { ArrowDownToLine, CircleCheck, LoaderCircle, RefreshCw, Square } from 'lucide-react';
import { Button } from './components/ui/button';
import { Badge } from './components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from './components/ui/card';
import { cn } from './lib/utils';
import type { LaunchStatus } from '../shared/launch';
import { BuildIdentity } from './components/build-identity';
const mib = (bytes: number) =>
  new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 1 }).format(bytes / 1048576) + ' МиБ';
export function InstallationCard({
  installation,
  lockReason,
  launch,
}: {
  installation: ReturnType<typeof useInstallation>;
  lockReason: string;
  launch: LaunchStatus | null;
}) {
  const { current, running, prepared, error, acting, start, cancel, reload } = installation;
  const retryingStatus = Boolean(error);
  const preparationLocked = !retryingStatus && Boolean(lockReason);
  const actionDisabled = acting || preparationLocked || (!current && !retryingStatus);
  return (
    <Card className="installation-card" aria-label="Подготовка файлов игры">
      <CardHeader>
        <div className="section-heading">
          <p className="card-eyebrow">02 / ФАЙЛЫ ИГРЫ</p>
          <Badge variant="outline" className={cn(prepared && !error && 'ready-badge')}>
            {error || current?.phase === 'error'
              ? 'Ошибка'
              : prepared
                ? 'Подготовлены'
                : running
                  ? 'Подготовка'
                  : !current
                    ? 'Проверяем…'
                    : current.phase === 'cancelled'
                      ? 'Отменено'
                      : 'Не проверены'}
          </Badge>
        </div>
        <CardTitle>
          <h2>Minecraft 1.8.9</h2>
        </CardTitle>
        <p className="installation-edition">Fabric Loader · Space Client</p>
      </CardHeader>
      <CardContent>
        <div
          className={cn('installation-status', current?.phase === 'error' && 'installation-error')}
          role={current?.phase === 'error' ? 'alert' : 'status'}
          aria-live={current?.phase === 'error' ? 'assertive' : 'polite'}
          aria-atomic="true"
        >
          {running ? (
            <LoaderCircle className="spinner" aria-hidden="true" />
          ) : prepared && !error ? (
            <CircleCheck aria-hidden="true" />
          ) : (
            <ArrowDownToLine aria-hidden="true" />
          )}
          <span>{current?.message || 'Проверяем состояние файлов…'}</span>
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
          {running ? (
            <Button
              variant="outline"
              disabled={acting || current?.phase === 'cancelling'}
              onClick={cancel}
            >
              <Square aria-hidden="true" />
              {current?.phase === 'cancelling' ? 'Останавливаем…' : 'Отменить подготовку'}
            </Button>
          ) : (
            <Button
              variant="outline"
              disabled={actionDisabled}
              onClick={retryingStatus ? reload : start}
              aria-describedby={preparationLocked ? 'files-availability' : undefined}
            >
              {prepared ? <RefreshCw aria-hidden="true" /> : <ArrowDownToLine aria-hidden="true" />}
              {error
                ? 'Повторить проверку состояния'
                : prepared
                  ? 'Проверить и восстановить'
                  : current?.phase === 'cancelled' || current?.phase === 'error'
                    ? 'Повторить подготовку'
                    : 'Подготовить файлы'}
            </Button>
          )}
        </div>
        {!running && preparationLocked && (
          <p id="files-availability" className="inline-hint">
            {lockReason}
          </p>
        )}
        {current && (
          <BuildIdentity bundled={current.bundledBuild} verified={launch?.verifiedBuild ?? null} />
        )}
      </CardContent>
    </Card>
  );
}
