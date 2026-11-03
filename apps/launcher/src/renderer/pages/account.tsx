import { useEffect, useState } from 'react';
import { RefreshCw, UserRound, LogOut, Upload, Trash2, Check } from 'lucide-react';
import { useLauncherApi } from '../hooks/launcher-api';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/card';
import type { AccountState, SkinModel } from '../../shared/account';
import type { Result } from '../../shared/models';
import type { ReadyLauncherModel } from '../model/launcher-session';

export function AccountPage({ model }: { model: ReadyLauncherModel }) {
  const api = useLauncherApi();
  const [account, setAccount] = useState<AccountState | null>(null),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(''),
    [notice, setNotice] = useState('');
  const [register, setRegister] = useState(false),
    [username, setUsername] = useState(''),
    [password, setPassword] = useState(''),
    [nickname, setNickname] = useState(''),
    [skinModel, setSkinModel] = useState<SkinModel>('classic'),
    [confirmed, setConfirmed] = useState(false);
  const applyState = (state: AccountState) => {
    setAccount(state);
    setNickname(state.profile?.nickname ?? '');
    setSkinModel(state.profile?.skin?.model ?? 'classic');
    setNotice(state.message);
  };
  const act = async (operation: () => Promise<Result<AccountState>>) => {
    if (busy) return;
    setBusy(true);
    setError('');
    setNotice('');
    try {
      const result = await operation();
      if (result.ok) applyState(result.value);
      else {
        setError(result.error);
        const fresh = await api.getAccount();
        if (fresh.ok) applyState(fresh.value);
      }
    } catch {
      setError('Не удалось связаться с лаунчером. Повторите действие.');
    } finally {
      setBusy(false);
    }
  };
  useEffect(() => {
    let stopped = false;
    void api
      .getAccount()
      .then((result) => {
        if (stopped) return;
        if (result.ok) applyState(result.value);
        else setError(result.error);
      })
      .catch(() => {
        if (!stopped) setError('Не удалось получить состояние Space.');
      });
    return () => {
      stopped = true;
    };
  }, [api]);
  const profile = account?.profile;
  const running =
    !!model.launch && ['checking', 'starting', 'running'].includes(model.launch.phase);
  const locked = busy || model.busy;
  const applyLocked = locked || running || model.dirty || model.nicknameDirty || !confirmed;
  return (
    <div className="account-workspace" aria-busy={busy}>
      <div
        className="account-feedback"
        role={error ? 'alert' : 'status'}
        aria-live={error ? 'assertive' : 'polite'}
      >
        {error || notice || (busy ? 'Выполняем действие Space…' : '')}
      </div>
      {!profile ? (
        <Card className="account-auth">
          <CardHeader>
            <p className="card-eyebrow">SPACE ACCOUNT</p>
            <CardTitle>
              <h2>{register ? 'Создать аккаунт Space' : 'Войти в Space'}</h2>
            </CardTitle>
            <CardDescription>
              Один профиль для сайта и лаунчера. Локальная игра доступна и без входа.
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="account-tabs" aria-label="Действие с аккаунтом">
              <Button
                variant="outline"
                aria-pressed={!register}
                disabled={locked}
                onClick={() => {
                  setRegister(false);
                  setError('');
                }}
              >
                Вход
              </Button>
              <Button
                variant="outline"
                aria-pressed={register}
                disabled={locked}
                onClick={() => {
                  setRegister(true);
                  setError('');
                }}
              >
                Регистрация
              </Button>
            </div>
            <form
              className="account-form"
              onSubmit={async (event) => {
                event.preventDefault();
                const input = { username, password };
                await act(() =>
                  register ? api.registerAccount({ ...input, nickname }) : api.loginAccount(input),
                );
                setPassword('');
              }}
            >
              <div className="form-field">
                <label htmlFor="space-username">Имя Space</label>
                <Input
                  id="space-username"
                  autoComplete="username"
                  required
                  minLength={3}
                  maxLength={24}
                  pattern="[A-Za-z0-9_]{3,24}"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  disabled={locked}
                  aria-describedby="space-handle-help"
                />
                <p id="space-handle-help" className="inline-hint">
                  3–24 латинских символа, цифры или _. Имя аккаунта постоянно.
                </p>
              </div>
              {register && (
                <div className="form-field">
                  <label htmlFor="space-initial-nickname">Игровой ник</label>
                  <Input
                    id="space-initial-nickname"
                    autoComplete="nickname"
                    required
                    minLength={3}
                    maxLength={16}
                    pattern="[A-Za-z0-9_]{3,16}"
                    value={nickname}
                    onChange={(e) => setNickname(e.target.value)}
                    disabled={locked}
                  />
                  <p className="inline-hint">
                    3–16 символов. Вход не заменит сохранённый локальный ник.
                  </p>
                </div>
              )}
              <div className="form-field">
                <label htmlFor="space-password">Пароль</label>
                <Input
                  id="space-password"
                  type="password"
                  autoComplete={register ? 'new-password' : 'current-password'}
                  required
                  minLength={15}
                  maxLength={256}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  disabled={locked}
                  aria-describedby="space-password-help"
                />
                <p id="space-password-help" className="inline-hint">
                  15–128 символов. Пробелы и вставка разрешены.
                </p>
              </div>
              <Button type="submit" disabled={locked}>
                {register ? 'Создать аккаунт' : 'Войти в Space'}
              </Button>
            </form>
            <p className="inline-hint">
              Сессия хранится только в защищённом хранилище Windows. Пароль не сохраняется.
            </p>
            <Button
              variant="ghost"
              disabled={locked}
              onClick={() => void act(() => api.logoutAccount())}
            >
              Удалить недоступную сохранённую сессию
            </Button>
          </CardContent>
        </Card>
      ) : (
        <>
          <Card className="account-profile">
            <CardHeader>
              <p className="card-eyebrow">SPACE ACCOUNT / @{profile.username}</p>
              <CardTitle>
                <h2>{profile.nickname}</h2>
              </CardTitle>
              <CardDescription>Профиль Space · ревизия {profile.revision}</CardDescription>
            </CardHeader>
            <CardContent>
              <form
                className="account-form"
                onSubmit={(e) => {
                  e.preventDefault();
                  void act(() => api.editAccount({ nickname, expectedRevision: profile.revision }));
                }}
              >
                <div className="form-field">
                  <label htmlFor="space-profile-nickname">Игровой ник Space</label>
                  <Input
                    id="space-profile-nickname"
                    autoComplete="nickname"
                    required
                    pattern="[A-Za-z0-9_]{3,16}"
                    minLength={3}
                    maxLength={16}
                    value={nickname}
                    disabled={locked}
                    onChange={(e) => setNickname(e.target.value)}
                  />
                </div>
                <Button
                  type="submit"
                  variant="outline"
                  disabled={locked || nickname === profile.nickname}
                >
                  Сохранить ник Space
                </Button>
              </form>
              <p className="inline-hint">
                Ник в игре сейчас: <strong>{model.snapshot.localNickname ?? 'не сохранён'}</strong>.
                Сохранение в Space не меняет его автоматически.
              </p>
              <div className="account-profile-actions">
                <Button
                  variant="outline"
                  disabled={locked}
                  onClick={() => void act(() => api.getAccount())}
                >
                  <RefreshCw aria-hidden="true" />
                  Обновить профиль
                </Button>
                <Button
                  variant="ghost"
                  disabled={locked}
                  onClick={() => void act(() => api.logoutAccount())}
                >
                  <LogOut aria-hidden="true" />
                  Выйти из Space
                </Button>
              </div>
            </CardContent>
          </Card>
          <div className="appearance-grid">
            {(['skin', 'cape'] as const).map((kind) => (
              <Card key={kind}>
                <CardHeader>
                  <CardTitle>
                    <h2>{kind === 'skin' ? 'Скин' : 'Плащ'}</h2>
                  </CardTitle>
                  <CardDescription>
                    {kind === 'skin' ? '64×64 или classic 64×32' : '64×32, 128×64 или 256×128'} ·
                    PNG до 256 КиБ, без чересстрочной развёртки
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="appearance-preview">
                    {(kind === 'skin' ? account.skinPreview : account.capePreview) ? (
                      <img
                        src={
                          'data:image/png;base64,' +
                          (kind === 'skin' ? account.skinPreview : account.capePreview)
                        }
                        alt={kind === 'skin' ? 'Развёртка скина Space' : 'Развёртка плаща Space'}
                      />
                    ) : (
                      <span>
                        <UserRound aria-hidden="true" />
                        Не выбран
                      </span>
                    )}
                  </div>
                  {kind === 'skin' && (
                    <div className="form-field">
                      <label htmlFor="space-skin-model">Модель скина</label>
                      <select
                        id="space-skin-model"
                        value={skinModel}
                        disabled={locked}
                        onChange={(e) => setSkinModel(e.target.value as SkinModel)}
                      >
                        <option value="classic">Classic</option>
                        <option value="slim">Slim</option>
                      </select>
                    </div>
                  )}
                  <div className="appearance-actions">
                    <Button
                      variant="outline"
                      disabled={locked}
                      onClick={() =>
                        void act(() =>
                          kind === 'skin'
                            ? api.uploadSkin({
                                expectedRevision: profile.revision,
                                model: skinModel,
                              })
                            : api.uploadCape({ expectedRevision: profile.revision }),
                        )
                      }
                    >
                      <Upload aria-hidden="true" />
                      Выбрать PNG
                    </Button>
                    <Button
                      variant="ghost"
                      disabled={locked || !profile[kind]}
                      onClick={() =>
                        void act(() =>
                          kind === 'skin'
                            ? api.deleteSkin(profile.revision)
                            : api.deleteCape(profile.revision),
                        )
                      }
                    >
                      <Trash2 aria-hidden="true" />
                      Удалить {kind === 'skin' ? 'скин' : 'плащ'}
                    </Button>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>
          <Card className="account-apply">
            <CardHeader>
              <CardTitle>
                <h2>Применить к игре</h2>
              </CardTitle>
              <CardDescription>Профиль «{model.profile.name}» · следующий запуск</CardDescription>
            </CardHeader>
            <CardContent>
              <p>
                Внешний вид применяется к твоему игроку в Space. Это не официальный скин Mojang и не
                обещание видимости во всех клиентах.
              </p>
              <label className="confirmation">
                <input
                  type="checkbox"
                  checked={confirmed}
                  onChange={(e) => setConfirmed(e.target.checked)}
                  disabled={locked}
                />
                <span>
                  Понимаю: смена ника может изменить offline identity, инвентарь и привязку данных
                  игрока. Миры остаются на месте.
                </span>
              </label>
              <Button
                disabled={applyLocked}
                onClick={async () => {
                  if (applyLocked) return;
                  setBusy(true);
                  setError('');
                  try {
                    const result = await api.applyAccount({
                      id: model.profile.id,
                      confirmIdentityChange: true,
                    });
                    if (result.ok) {
                      applyState(result.value.account);
                      setConfirmed(false);
                      await model.session.load();
                    } else setError(result.error);
                  } catch {
                    setError('Не удалось применить профиль. Повторите.');
                  } finally {
                    setBusy(false);
                  }
                }}
              >
                <Check aria-hidden="true" />
                Применить профиль Space
              </Button>
              {(running || model.dirty || model.nicknameDirty) && (
                <p className="inline-hint">
                  Завершите игру и сохраните или отмените черновики профиля и ника. Во время
                  подготовки файлов действие также недоступно.
                </p>
              )}
            </CardContent>
          </Card>
        </>
      )}
    </div>
  );
}
