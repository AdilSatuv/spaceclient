import { useInstallation } from './hooks/use-installation';
import {
  ArrowDownToLine,
  Check,
  CircleCheck,
  LoaderCircle,
  LockKeyhole,
  RefreshCw,
  Square,
} from 'lucide-react';
import { Button } from './components/ui/button';
import { Badge } from './components/ui/badge';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './components/ui/card';
import { cn } from './lib/utils';

const mib = (bytes: number) =>
  new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 1 }).format(bytes / 1048576) + ' МиБ';
export function InstallationCard({ profileId, dirty }: { profileId: string; dirty: boolean }) {
  const { current, running, prepared, error, acting, start, cancel } = useInstallation(profileId);
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
              disabled={acting || current?.phase === 'cancelling'}
              onClick={cancel}
            >
              <Square aria-hidden="true" />
              {current?.phase === 'cancelling' ? 'Останавливаем…' : 'Отменить подготовку'}
            </Button>
          ) : (
            <Button disabled={acting || dirty || !current} onClick={start}>
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
