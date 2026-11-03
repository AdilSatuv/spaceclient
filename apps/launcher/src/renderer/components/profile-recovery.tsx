import { useState } from 'react';
import { useLauncherApi } from '../hooks/launcher-api';
import { Button } from './ui/button';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from './ui/card';
import type { ReadyLauncherModel } from '../model/launcher-session';

export function ProfileRecoveryCard({ model }: { model: ReadyLauncherModel }) {
  const api = useLauncherApi(),
    [busy, setBusy] = useState(false),
    [confirmed, setConfirmed] = useState(false),
    [notice, setNotice] = useState(''),
    [error, setError] = useState('');
  const running =
      !!model.launch && ['checking', 'starting', 'running'].includes(model.launch.phase),
    locked = busy || model.busy || running || model.dirty || model.nicknameDirty;
  const act = async (kind: 'backup' | 'restore' | 'diagnostics') => {
    if (locked) return;
    setBusy(true);
    setError('');
    setNotice('');
    try {
      if (kind === 'restore') {
        const result = await api.restoreProfile({ id: model.profile.id, confirmed: true });
        if (!result.ok) {
          setError(result.error);
          return;
        }
        setNotice(result.value.result.message);
        setConfirmed(false);
        await model.session.load();
      } else {
        const result = await (kind === 'backup'
          ? api.backupProfile(model.profile.id)
          : api.exportDiagnostics(model.profile.id));
        if (result.ok) setNotice(result.value.message);
        else setError(result.error);
      }
    } catch {
      setError('Не удалось выполнить действие. Повторите.');
    } finally {
      setBusy(false);
    }
  };
  return (
    <Card className="profile-recovery">
      <CardHeader>
        <CardTitle>
          <h2>Сохранить и восстановить</h2>
        </CardTitle>
        <CardDescription>Только профиль «{model.profile.name}»</CardDescription>
      </CardHeader>
      <CardContent>
        <p>
          Копия включает память и название профиля, options и настройки Space: HUD, моды, каталог,
          метки и косметику. Миры, аккаунты, Java и журналы не копируются. Новая копия заменяет
          предыдущую.
        </p>
        <div className="recovery-actions">
          <Button variant="outline" disabled={locked} onClick={() => void act('backup')}>
            Создать резервную копию
          </Button>
          <Button variant="outline" disabled={locked} onClick={() => void act('diagnostics')}>
            Экспорт диагностики
          </Button>
        </div>
        <p className="inline-hint">
          Диагностика содержит только технические версии и состояния, без ников, путей, токенов и
          журналов.
        </p>
        <label className="confirmation">
          <input
            type="checkbox"
            checked={confirmed}
            onChange={(e) => setConfirmed(e.target.checked)}
            disabled={locked}
          />
          <span>Заменить текущие настройки этого профиля последней резервной копией.</span>
        </label>
        <Button
          variant="outline"
          disabled={locked || !confirmed}
          onClick={() => void act('restore')}
        >
          Восстановить настройки
        </Button>
        {locked && !busy && (
          <p className="inline-hint">
            Завершите игру и сохраните или отмените черновики. Во время подготовки файлов действие
            также заблокировано.
          </p>
        )}
        <p
          role={error ? 'alert' : 'status'}
          className={error ? 'installation-error' : 'inline-hint'}
        >
          {error || notice || (busy ? 'Проверяем и сохраняем файлы…' : '')}
        </p>
      </CardContent>
    </Card>
  );
}
