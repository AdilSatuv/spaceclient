import { useRef } from 'react';
import { Play, UserRound } from 'lucide-react';
import { Button } from './components/ui/button';
import { Input } from './components/ui/input';
import { Badge } from './components/ui/badge';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './components/ui/card';
import type { ReadyLauncherModel } from './model/launcher-session';

export function LaunchCard({ model }: { model: ReadyLauncherModel }) {
  const { session, profile, snapshot, busy, dirty, nicknameDraft, nicknameDirty, nicknameError } =
    model;
  const input = useRef<HTMLInputElement>(null);
  const current = model.launch?.profileId === profile.id ? model.launch : null;
  const running = !!current && ['checking', 'starting', 'running'].includes(current.phase);
  return (
    <Card className="local-launch" aria-label="Локальный вход и запуск">
      <CardHeader>
        <div className="installation-heading">
          <UserRound aria-hidden="true" />
          <CardTitle>
            <h2>В игру со своим ником.</h2>
          </CardTitle>
          <Badge variant="outline">Локальный режим</Badge>
        </div>
        <CardDescription>
          Ник хранится на этом компьютере. Это локальная игра без регистрации Space и входа
          Microsoft. Доступ к серверам с проверкой аккаунта не предоставляется.
        </CardDescription>
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
            <label htmlFor="local-nickname">Твой ник</label>
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
          <Button
            type="submit"
            variant="outline"
            disabled={busy || running || dirty || !nicknameDirty}
          >
            Сохранить ник
          </Button>
          <Button
            className="launch-button"
            type="button"
            disabled={
              busy || dirty || nicknameDirty || !snapshot.localNickname || !current || running
            }
            onClick={session.startLaunch}
          >
            <Play aria-hidden="true" />
            {running
              ? current?.phase === 'running'
                ? 'Игра работает'
                : 'Запускаем…'
              : 'Играть локально'}
          </Button>
        </form>
        <p className="inline-hint" role="status" aria-live="polite">
          {current?.message ?? 'Проверяем состояние игры…'}
          {current?.exitCode !== null && current?.exitCode !== undefined
            ? ` Код выхода: ${current.exitCode}.`
            : ''}
        </p>
        {(dirty || nicknameDirty) && (
          <p className="inline-hint">Сохрани изменения перед запуском.</p>
        )}
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
