'use client';
import { useEffect, useRef, useState } from 'react';
import type { SpaceAccountProfile } from '@space/account-contracts';
import { NICKNAME_PATTERN, USERNAME_PATTERN } from '@space/account-contracts';
import { requestAccount, AccountBrowserError } from '@/shared/api/index.client';
import MediaEditor from './media-editor';

export default function AccountPanel() {
  const [profile, setProfile] = useState<SpaceAccountProfile | null>(null),
    [csrf, setCsrf] = useState(''),
    [ready, setReady] = useState(false),
    [busy, setBusy] = useState(false),
    [mode, setMode] = useState<'login' | 'register'>('login'),
    [error, setError] = useState(''),
    [notice, setNotice] = useState(''),
    [nickname, setNickname] = useState(''),
    [invalid, setInvalid] = useState('');
  const alert = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (error) alert.current?.focus();
  }, [error]);
  const refresh = async () => {
    setBusy(true);
    setError('');
    try {
      const data = await requestAccount('session');
      setProfile(data.profile ?? null);
      setNickname(data.profile?.nickname ?? '');
      setCsrf(data.csrfToken ?? '');
      setReady(true);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Не удалось загрузить профиль.');
    } finally {
      setBusy(false);
    }
  };
  useEffect(() => {
    void refresh();
  }, []);
  const mutate = async (action: Parameters<typeof requestAccount>[0], body?: unknown) => {
    setBusy(true);
    setError('');
    setNotice('');
    setInvalid('');
    try {
      const data = await requestAccount(action, body, csrf);
      if (data.profile) {
        setProfile(data.profile);
        setNickname(data.profile.nickname);
      }
      if (action === 'logout') {
        setProfile(null);
        setNickname('');
        setMode('login');
      }
      setNotice(
        action === 'logout'
          ? 'Вы вышли из аккаунта.'
          : action === 'login' || action === 'register'
            ? 'Вы вошли в Space.'
            : 'Изменения сохранены в аккаунте Space.',
      );
      return true;
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Не удалось сохранить изменение.');
      if (e instanceof AccountBrowserError && e.code === 'SPACE_UNAUTHORIZED') setProfile(null);
      if (e instanceof AccountBrowserError && e.localLogout) {
        setProfile(null);
        setNickname('');
        setMode('login');
      }
      return false;
    } finally {
      setBusy(false);
    }
  };
  const fail = (message: string, field = '') => {
    setInvalid(field);
    setError(message);
    setNotice('');
  };
  return (
    <div className="account-workspace" aria-busy={busy}>
      {error && (
        <div className="account-message account-error" role="alert" tabIndex={-1} ref={alert}>
          <strong>Не удалось выполнить действие</strong>
          <p>{error}</p>
          {invalid && (
            <a className="text-link" href={`#${invalid}`}>
              Перейти к полю
            </a>
          )}
          <button className="button secondary" onClick={() => void refresh()} disabled={busy}>
            Обновить профиль
          </button>
        </div>
      )}
      {notice && (
        <p className="account-message" role="status">
          {notice}
        </p>
      )}
      {!ready ? (
        <p role="status">
          {busy
            ? 'Загружаем аккаунт…'
            : 'Сервис аккаунтов недоступен. Обновите профиль, чтобы повторить.'}
        </p>
      ) : !profile ? (
        <div className="account-entry">
          <section className="page-panel">
            <div className="account-tabs" aria-label="Вход или регистрация">
              <button
                className="button secondary"
                aria-pressed={mode === 'login'}
                onClick={() => {
                  setMode('login');
                  setError('');
                  setInvalid('');
                }}
              >
                Войти
              </button>
              <button
                className="button secondary"
                aria-pressed={mode === 'register'}
                onClick={() => {
                  setMode('register');
                  setError('');
                  setInvalid('');
                }}
              >
                Создать аккаунт
              </button>
            </div>
            <h2>{mode === 'login' ? 'С возвращением' : 'Твоя орбита начинается здесь'}</h2>
            <form
              key={mode}
              onSubmit={async (event) => {
                event.preventDefault();
                const form = event.currentTarget,
                  values = new FormData(form),
                  username = String(values.get('username') ?? ''),
                  password = String(values.get('password') ?? ''),
                  name = String(values.get('nickname') ?? '');
                if (!USERNAME_PATTERN.test(username)) {
                  fail('Имя аккаунта: 3–24 латинских буквы, цифры или _.', 'account-username');
                  return;
                }
                if ([...password].length < 15 || [...password].length > 128) {
                  fail('Пароль: от 15 до 128 символов. Пробелы разрешены.', 'account-password');
                  return;
                }
                if (mode === 'register' && !NICKNAME_PATTERN.test(name)) {
                  fail('Ник: 3–16 латинских букв, цифр или _.', 'account-nickname');
                  return;
                }
                if (mode === 'register' && password !== String(values.get('confirm') ?? '')) {
                  fail('Пароли не совпадают.', 'account-confirm');
                  return;
                }
                if (
                  await mutate(mode, {
                    username,
                    password,
                    ...(mode === 'register' ? { nickname: name } : {}),
                  })
                )
                  form.reset();
              }}
              noValidate
            >
              <fieldset disabled={busy}>
                <legend className="visually-hidden">
                  {mode === 'login' ? 'Вход в Space' : 'Регистрация Space'}
                </legend>
                <label htmlFor="account-username">
                  Имя аккаунта
                  <input
                    id="account-username"
                    name="username"
                    autoComplete="username"
                    autoCapitalize="none"
                    maxLength={24}
                    aria-invalid={invalid === 'account-username'}
                    aria-describedby="username-help"
                    required
                  />
                </label>
                <p id="username-help" className="caption">
                  3–24 латинских буквы, цифры или _. Имя аккаунта изменить нельзя.
                </p>
                {mode === 'register' && (
                  <>
                    <label htmlFor="account-nickname">
                      Ник в Space
                      <input
                        id="account-nickname"
                        name="nickname"
                        autoComplete="nickname"
                        maxLength={16}
                        aria-invalid={invalid === 'account-nickname'}
                        aria-describedby="nickname-help"
                        required
                      />
                    </label>
                    <p id="nickname-help" className="caption">
                      3–16 латинских букв, цифр или _. Можно изменить после регистрации.
                    </p>
                  </>
                )}
                <label htmlFor="account-password">
                  Пароль
                  <input
                    id="account-password"
                    name="password"
                    type="password"
                    autoComplete={mode === 'register' ? 'new-password' : 'current-password'}
                    maxLength={256}
                    aria-invalid={invalid === 'account-password'}
                    aria-describedby="password-help"
                    required
                  />
                </label>
                <p id="password-help" className="caption">
                  15–128 символов. Можно вставлять пароль и использовать пробелы.
                </p>
                {mode === 'register' && (
                  <label htmlFor="account-confirm">
                    Повторите пароль
                    <input
                      id="account-confirm"
                      name="confirm"
                      type="password"
                      autoComplete="new-password"
                      aria-invalid={invalid === 'account-confirm'}
                      required
                    />
                  </label>
                )}
                <button className="button" type="submit">
                  {busy ? 'Подождите…' : mode === 'login' ? 'Войти в Space' : 'Зарегистрироваться'}
                </button>
              </fieldset>
            </form>
          </section>
          <aside className="account-explainer">
            <p className="eyebrow">ОДИН АККАУНТ</p>
            <h2>Сайт и лаунчер связаны.</h2>
            <p>
              Изменения сохраняются на общем локальном сервере Space. Войдите в тот же аккаунт в
              лаунчере, чтобы использовать свой профиль.
            </p>
            <p>
              Скин и плащ сначала видны вашему игроку в Space. Это не официальный скин Minecraft и
              не обещание видимости у других клиентов.
            </p>
            <p className="caption">
              Восстановление пароля и подтверждение почты пока не добавлены. Сохраните пароль в
              менеджере паролей.
            </p>
          </aside>
        </div>
      ) : (
        <>
          <section className="page-panel account-profile">
            <div>
              <p className="eyebrow">АККАУНТ SPACE · {profile.username}</p>
              <h2>{profile.nickname}</h2>
              <p className="caption">
                Версия профиля {profile.revision}. Имя аккаунта: {profile.username}.
              </p>
            </div>
            <div className="account-actions">
              <button className="button secondary" disabled={busy} onClick={() => void refresh()}>
                Обновить профиль
              </button>
              <button
                className="button secondary"
                disabled={busy}
                onClick={() => void mutate('logout')}
              >
                Выйти
              </button>
            </div>
          </section>
          <section className="page-panel account-nickname">
            <h2>Ник в игре</h2>
            <form
              onSubmit={(event) => {
                event.preventDefault();
                if (!NICKNAME_PATTERN.test(nickname)) {
                  fail('Ник: 3–16 латинских букв, цифр или _.', 'profile-nickname');
                  return;
                }
                void mutate('rename', { nickname, expectedRevision: profile.revision });
              }}
            >
              <fieldset disabled={busy}>
                <legend className="visually-hidden">Изменить ник</legend>
                <label htmlFor="profile-nickname">
                  Новый ник
                  <input
                    id="profile-nickname"
                    name="nickname"
                    value={nickname}
                    onChange={(event) => setNickname(event.target.value)}
                    maxLength={16}
                    aria-invalid={invalid === 'profile-nickname'}
                    aria-describedby="profile-nickname-help"
                  />
                </label>
                <button className="button" type="submit">
                  Сохранить ник
                </button>
              </fieldset>
            </form>
            <p id="profile-nickname-help" className="caption">
              Изменение сохраняется в аккаунте. Примените профиль в лаунчере перед следующим
              запуском. Смена локального ника может изменить игровую личность и привязку инвентаря.
            </p>
          </section>
          <div className="account-media-grid">
            <MediaEditor kind="skin" profile={profile} disabled={busy} save={mutate} fail={fail} />
            <MediaEditor kind="cape" profile={profile} disabled={busy} save={mutate} fail={fail} />
          </div>
          <p className="caption">
            Изменения не затрагивают запущенную игру. Обновите профиль и примените его в лаунчере
            перед следующим запуском.
          </p>
        </>
      )}
    </div>
  );
}
