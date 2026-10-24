import type { useInstallation } from './hooks/use-installation';
import {
  ArrowDownToLine,
  CircleCheck,
  LoaderCircle,
  RefreshCw,
  ShieldCheck,
  Square,
} from 'lucide-react';
import { Button } from './components/ui/button';
import { Badge } from './components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from './components/ui/card';
import { cn } from './lib/utils';
const mib = (bytes: number) =>
  new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 1 }).format(bytes / 1048576) + ' МиБ';
export function InstallationCard({
  installation,
  locked,
}: {
  installation: ReturnType<typeof useInstallation>;
  locked: boolean;
}) {
  const { current, running, prepared, error, acting, start, cancel } = installation;
  return (
    <Card className="installation-card" aria-label="Подготовка файлов игры">
      <CardHeader>
        <div className="section-heading">
          <p className="card-eyebrow">02 / ФАЙЛЫ ИГРЫ</p>
          <Badge variant="outline" className={cn(prepared && 'ready-badge')}>
            {prepared ? 'Проверены' : running ? 'Подготовка' : 'Не проверены'}
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
          role="status"
          aria-live="polite"
          aria-atomic="true"
        >
          {running ? (
            <LoaderCircle className="spinner" aria-hidden="true" />
          ) : prepared ? (
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
            <Button variant="outline" disabled={acting || locked || !current} onClick={start}>
              {prepared ? <RefreshCw aria-hidden="true" /> : <ArrowDownToLine aria-hidden="true" />}
              {prepared
                ? 'Проверить и восстановить'
                : current?.phase === 'cancelled' || current?.phase === 'error'
                  ? 'Продолжить подготовку'
                  : 'Подготовить файлы'}
            </Button>
          )}
        </div>
        <div className="installation-footnote">
          <ShieldCheck aria-hidden="true" />
          <p>
            Отдельная папка профиля.
            <br />
            Файлы проверяются перед каждым запуском.
          </p>
        </div>
        {current?.spaceVersion && (
          <p className="build-note">SPACE {current.spaceVersion} / DEVELOPMENT</p>
        )}
      </CardContent>
    </Card>
  );
}
