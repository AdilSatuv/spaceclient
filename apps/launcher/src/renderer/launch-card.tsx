import { useRef } from 'react';
import { ArrowUpRight, Check, Cpu, LoaderCircle, Play, Terminal } from 'lucide-react';
import { Button } from './components/ui/button';
import { Input } from './components/ui/input';
import { Badge } from './components/ui/badge';
import { Card, CardContent, CardHeader, CardTitle } from './components/ui/card';
import { gb } from './lib/format';
import type { ReadyLauncherModel } from './model/launcher-session';
import { launchAvailability } from './model/launch-availability';

export function LaunchCard({
  model,
  onProfiles,
  preparing,
}: {
  model: ReadyLauncherModel;
  onProfiles: () => void;
  preparing: boolean;
}) {
  const { session, profile, busy, dirty, nicknameDraft, nicknameDirty, nicknameError, javaLabel } =
    model;
  const input = useRef<HTMLInputElement>(null);
  const current = model.launch?.profileId === profile.id ? model.launch : null;
  const running = !!current && ['checking', 'starting', 'running'].includes(current.phase);
  const availability = launchAvailability(model, preparing);
  return (
    <Card className="local-launch current-profile" aria-label="Локальный вход и запуск">
      <CardHeader>
        <div className="section-heading">
          <p className="card-eyebrow">01 / ТВОЙ ПРОФИЛЬ</p>
          <Badge variant="outline">Локально</Badge>
        </div>
        <CardTitle>
          <h2>{profile.name}</h2>
        </CardTitle>
        <div className="launch-profile-meta">
          <span>
            <Cpu aria-hidden="true" />
            {gb(profile.memoryMb)}
          </span>
          <span>
            <Terminal aria-hidden="true" />
            {javaLabel}
          </span>
          <Button variant="ghost" onClick={onProfiles} aria-label="Настроить активный профиль">
            Настроить
            <ArrowUpRight aria-hidden="true" />
          </Button>
        </div>
      </CardHeader>
      <CardContent>
        <form
          className="nickname-form"
          onSubmit={async (event) => {
            event.preventDefault();
            if (!session.validateNickname()) {
              input.current?.focus();
              return;
            }
            await session.saveNickname();
          }}
        >
          <div className="nickname-field">
            <label htmlFor="local-nickname">Ник в игре</label>
            <div className="nickname-controls">
              <Input
                ref={input}
                id="local-nickname"
                value={nicknameDraft}
                maxLength={16}
                autoComplete="off"
                spellCheck={false}
                disabled={busy || running}
                aria-invalid={!!nicknameError}
                aria-describedby="nickname-help nickname-error"
                onChange={(event) => session.editNickname(event.target.value)}
                onBlur={session.validateNickname}
              />
              <Button
                type="submit"
                variant="outline"
                disabled={busy || running || dirty || !nicknameDirty}
              >
                <Check aria-hidden="true" />
                Сохранить ник
              </Button>
            </div>
            <p id="nickname-help" className="inline-hint">
              3–16 символов: A–Z, a–z, 0–9, _
            </p>
            <p
              id="nickname-error"
              className="installation-error"
              role={nicknameError ? 'alert' : undefined}
            >
              {nicknameError}
            </p>
          </div>
          <p id="launch-availability" className="inline-hint" role="status" aria-live="polite">
            {availability.reason}
          </p>
          <Button
            className="launch-button"
            type="button"
            disabled={availability.disabled}
            aria-describedby="launch-availability launch-status"
            onClick={session.startLaunch}
          >
            {running && current?.phase !== 'running' ? (
              <LoaderCircle className="spinner" aria-hidden="true" />
            ) : (
              <Play aria-hidden="true" />
            )}
            {running
              ? current?.phase === 'running'
                ? 'Игра работает'
                : 'Запускаем…'
              : preparing
                ? 'Подготовка файлов…'
                : 'Играть локально'}
            {!running && !preparing && <ArrowUpRight aria-hidden="true" />}
          </Button>
        </form>
        <p
          id="launch-status"
          className="launch-status"
          data-phase={current?.phase ?? 'idle'}
          role="status"
          aria-live="polite"
        >
          {current?.message ?? 'Проверяем состояние игры…'}
          {current?.exitCode !== null && current?.exitCode !== undefined
            ? ` Код выхода: ${current.exitCode}.`
            : ''}
        </p>
        <p className="local-mode-note">
          Ник хранится на этом компьютере. Без регистрации Space и входа Microsoft; серверы с
          проверкой аккаунта недоступны.
        </p>
        {!!current?.log.length && (
          <details className="launch-journal">
            <summary>Журнал запуска</summary>
            <ul>
              {current.log.map((line, index) => (
                <li key={index}>{line}</li>
              ))}
            </ul>
          </details>
        )}
      </CardContent>
    </Card>
  );
}
