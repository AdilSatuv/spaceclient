import { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import type { JavaInfo, LauncherApi, ProfileEdit, Result, Snapshot } from '../shared/contracts';
import './style.css';
import { InstallationCard } from './installation-card';

declare global {
  interface Window {
    space: LauncherApi;
  }
}
type View = 'home' | 'profiles' | 'settings';
function Orbit({ small = false }: { small?: boolean }) {
  return (
    <svg
      className={small ? 'orbit-logo small' : 'orbit-logo'}
      viewBox="0 0 48 48"
      fill="none"
      aria-hidden="true"
    >
      <path d="m24 7 4.5 12.5L41 24l-12.5 4.5L24 41l-4.5-12.5L7 24l12.5-4.5Z" fill="currentColor" />
      <ellipse
        cx="24"
        cy="24"
        rx="24"
        ry="8"
        transform="rotate(-42 24 24)"
        stroke="currentColor"
        strokeWidth="1.4"
      />
    </svg>
  );
}
function Icon({ kind }: { kind: View | 'arrow' | 'folder' }) {
  const paths = {
    home: 'm3 10 9-7 9 7v11h-6v-7H9v7H3z',
    profiles: 'M4 5h16v14H4zM8 9h8M8 13h5',
    settings: 'M4 7h16M4 17h16M9 4v6m6 4v6',
    arrow: 'M5 12h14m-5-5 5 5-5 5',
    folder: 'M3 6h7l2 3h9v11H3z',
  };
  return (
    <svg
      className="icon"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.6"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d={paths[kind]} />
    </svg>
  );
}
const gb = (mb: number) =>
  new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 2 }).format(mb / 1024) + ' ГБ';
function App() {
  const [snapshot, setSnapshot] = useState<Snapshot | null>(null);
  const [view, setView] = useState<View>('home');
  const [draft, setDraft] = useState<ProfileEdit | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [java, setJava] = useState<Record<string, JavaInfo>>({});
  const profile = snapshot?.profiles.find((item) => item.id === snapshot.activeProfileId);
  const dirty = !!(
    draft &&
    profile &&
    (draft.name !== profile.name || draft.memoryMb !== profile.memoryMb)
  );
  const apply = (value: Snapshot) => {
    setSnapshot(value);
    const active = value.profiles.find((item) => item.id === value.activeProfileId)!;
    setDraft({ id: active.id, name: active.name, memoryMb: active.memoryMb });
  };
  const run = async <T,>(action: () => Promise<Result<T>>, success: (value: T) => void) => {
    setBusy(true);
    setError('');
    setNotice('');
    try {
      const result = await action();
      if (result.ok) success(result.value);
      else setError(result.error);
    } catch {
      setError('Не удалось связаться с лаунчером. Перезапустите приложение.');
    } finally {
      setBusy(false);
    }
  };
  const load = () => run(() => window.space.getState(), apply);
  useEffect(() => {
    void load();
  }, []);
  const changeView = (next: View) => {
    setView(next);
    setError('');
    setNotice('');
  };
  const checkJava = () =>
    profile &&
    run(
      () => window.space.checkJava(profile.id),
      (result) => {
        setJava((current) => ({ ...current, [profile.id]: result }));
        setNotice(result.message);
      },
    );

  return (
    <>
      <div className="titlebar">
        <span className="title-mark">✦</span> SPACE CLIENT{' '}
        <span className="title-version">Лаунчер · 0.1.0</span>
      </div>
      <div className="app-shell">
        <aside className="sidebar">
          <div className="brand">
            <Orbit />
            <span>
              SPACE<span className="brand-sub">CLIENT</span>
            </span>
          </div>
          <div className="nav-label">ТВОЯ ОРБИТА</div>
          <nav aria-label="Разделы лаунчера">
            {(
              [
                ['home', 'Обзор'],
                ['profiles', 'Профили'],
                ['settings', 'Настройки'],
              ] as const
            ).map(([id, label]) => (
              <button
                key={id}
                aria-current={view === id ? 'page' : undefined}
                onClick={() => changeView(id)}
              >
                <Icon kind={id} />
                {label}
                {id === 'profiles' && dirty && (
                  <span className="draft-dot" aria-label="Есть несохранённые изменения" />
                )}
              </button>
            ))}
          </nav>
          <div className="sidebar-bottom">
            <div className="local-profile">
              <span className="avatar">
                <Orbit small />
              </span>
              <div>
                <strong>Локальный профиль</strong>
                <span>Аккаунт не подключён</span>
              </div>
            </div>
            <div className="build-label">
              <span /> Версия в разработке
            </div>
          </div>
        </aside>
        <main id="main" aria-busy={busy}>
          <header className="page-header">
            <div>
              <p className="eyebrow">
                SPACE CLIENT /{' '}
                {view === 'home'
                  ? 'НАЧАЛО ПУТИ'
                  : view === 'profiles'
                    ? 'ТВОИ ПРОФИЛИ'
                    : 'ТВОЁ УСТРОЙСТВО'}
              </p>
              <h1>
                {view === 'home'
                  ? 'Добро пожаловать на борт.'
                  : view === 'profiles'
                    ? 'Каждой игре — своя орбита.'
                    : 'Настроим твою орбиту.'}
              </h1>
            </div>
            <span className="version-pill">MINECRAFT 1.8.9</span>
          </header>
          {error && (
            <div className="feedback error" role="alert">
              {error}
            </div>
          )}
          <div className="feedback-slot" role="status" aria-live="polite">
            {busy ? <span>Выполняем действие…</span> : notice && <span>{notice}</span>}
          </div>
          {snapshot?.warning && <div className="feedback warning">{snapshot.warning}</div>}
          {!snapshot ? (
            <section className="panel startup">
              <Orbit />
              <h2>{busy ? 'Поднимаемся на орбиту…' : 'Не удалось загрузить профиль'}</h2>
              <p>Настройки хранятся на этом устройстве.</p>
              {!busy && (
                <button className="primary" onClick={load}>
                  Повторить
                </button>
              )}
            </section>
          ) : (
            profile &&
            draft && (
              <>
                {view === 'home' && (
                  <>
                    <section className="launch-hero">
                      <div className="hero-art" aria-hidden="true" />
                      <div className="hero-copy">
                        <span className="hero-tag">
                          <span /> НОВАЯ ОРБИТА MINECRAFT
                        </span>
                        <h2>
                          Твой мир.
                          <br />
                          <em>Твой космос.</em>
                        </h2>
                        <p>
                          Знакомая игра. Своя атмосфера.
                          <br />
                          Настрой профиль для первого полёта.
                        </p>
                        <div className="hero-actions">
                          <button className="primary" onClick={() => changeView('profiles')}>
                            Настроить профиль <Icon kind="arrow" />
                          </button>
                          <span>Java Edition · Windows x64</span>
                        </div>
                      </div>
                      <span className="hero-coordinate" aria-hidden="true">
                        01 / SPACE UNIVERSE
                      </span>
                    </section>
                    <div className="home-grid">
                      <section className="panel profile-card">
                        <div className="section-heading">
                          <span className="eyebrow">ТЕКУЩИЙ ПРОФИЛЬ</span>
                          <Icon kind="profiles" />
                        </div>
                        <h2>{profile.name}</h2>
                        <dl>
                          <div>
                            <dt>Память</dt>
                            <dd>{gb(profile.memoryMb)}</dd>
                          </div>
                          <div>
                            <dt>Java</dt>
                            <dd>
                              {java[profile.id]?.usable
                                ? 'Проверена'
                                : profile.javaPath
                                  ? 'Нужна проверка'
                                  : 'Не выбрана'}
                            </dd>
                          </div>
                        </dl>
                        <button className="text-button" onClick={() => changeView('profiles')}>
                          Управлять профилем <Icon kind="arrow" />
                        </button>
                      </section>
                      <InstallationCard profileId={profile.id} dirty={dirty} />
                    </div>
                  </>
                )}
                {view === 'profiles' && (
                  <div className="profiles-grid">
                    <section className="panel profile-list">
                      <div className="section-heading">
                        <h2>Профили</h2>
                        <span className="count">{snapshot.profiles.length}/10</span>
                      </div>
                      <p className="muted small-text">
                        Свои настройки и папка игры для каждой орбиты.
                      </p>
                      {snapshot.profiles.map((item) => (
                        <button
                          className="profile-option"
                          key={item.id}
                          disabled={busy || (dirty && item.id !== profile.id)}
                          aria-pressed={item.id === profile.id}
                          onClick={() => {
                            if (item.id !== profile.id)
                              void run(() => window.space.selectProfile(item.id), apply);
                          }}
                        >
                          <span className="profile-symbol">
                            <Orbit small />
                          </span>
                          <span>
                            <strong>{item.name}</strong>
                            <small>{gb(item.memoryMb)} · Minecraft 1.8.9</small>
                          </span>
                          {item.id === profile.id && (
                            <span className="selected-mark" aria-hidden="true">
                              ●
                            </span>
                          )}
                        </button>
                      ))}
                      <button
                        className="secondary full"
                        disabled={busy || dirty || snapshot.profiles.length >= 10}
                        onClick={() =>
                          run(
                            () => window.space.createProfile(),
                            (value) => {
                              apply(value);
                              setNotice('Новый профиль создан.');
                            },
                          )
                        }
                      >
                        + Создать профиль
                      </button>
                      {dirty && (
                        <p className="inline-hint">
                          Сохраните или отмените изменения, чтобы переключить профиль.
                        </p>
                      )}
                    </section>
                    <section className="panel profile-form">
                      <div className="section-heading">
                        <h2>Настройки профиля</h2>
                        <span className="status-chip">Локально</span>
                      </div>
                      <form
                        onSubmit={(event) => {
                          event.preventDefault();
                          void run(
                            () => window.space.saveProfile(draft),
                            (value) => {
                              apply(value);
                              setNotice('Профиль сохранён.');
                            },
                          );
                        }}
                      >
                        <label htmlFor="profile-name">Название</label>
                        <input
                          id="profile-name"
                          maxLength={40}
                          required
                          disabled={busy}
                          value={draft.name}
                          onChange={(event) => setDraft({ ...draft, name: event.target.value })}
                        />
                        <div className="range-heading">
                          <label htmlFor="memory">Память для игры</label>
                          <output htmlFor="memory">{gb(draft.memoryMb)}</output>
                        </div>
                        <input
                          id="memory"
                          type="range"
                          min={1024}
                          max={snapshot.maxMemoryMb}
                          step={256}
                          disabled={busy}
                          value={draft.memoryMb}
                          onChange={(event) =>
                            setDraft({ ...draft, memoryMb: Number(event.target.value) })
                          }
                        />
                        <div className="range-limits">
                          <span>1 ГБ</span>
                          <span>{gb(snapshot.maxMemoryMb)}</span>
                        </div>
                        <p className="inline-hint">
                          На устройстве {gb(snapshot.totalMemoryMb)}. Часть памяти оставляем
                          системе.
                        </p>
                        <div className="form-actions">
                          <button
                            className="primary"
                            disabled={busy || !dirty || !draft.name.trim()}
                          >
                            Сохранить изменения
                          </button>
                          <button
                            type="button"
                            className="secondary"
                            disabled={busy || !dirty}
                            onClick={() => {
                              apply(snapshot);
                              setError('');
                              setNotice('Изменения отменены.');
                            }}
                          >
                            Отменить
                          </button>
                        </div>
                      </form>
                      <div className="java-section">
                        <div className="section-heading">
                          <h3>Java для этого профиля</h3>
                          <span className="status-chip">21 · 64 бита</span>
                        </div>
                        <p className="file-path">
                          {profile.javaPath || 'Файл java.exe ещё не выбран'}
                        </p>
                        <div className="form-actions">
                          <button
                            className="secondary"
                            disabled={busy || dirty}
                            onClick={() =>
                              run(
                                () => window.space.chooseJava(profile.id),
                                (value) => {
                                  apply(value);
                                  setJava((current) => {
                                    const next = { ...current };
                                    delete next[profile.id];
                                    return next;
                                  });
                                },
                              )
                            }
                          >
                            Выбрать Java
                          </button>
                          <button
                            className="secondary"
                            disabled={busy || dirty || !profile.javaPath}
                            onClick={checkJava}
                          >
                            Проверить Java
                          </button>
                        </div>
                        {java[profile.id] && (
                          <p
                            className={
                              java[profile.id].usable ? 'java-result success' : 'java-result'
                            }
                            role="status"
                          >
                            {java[profile.id].message}
                          </p>
                        )}
                      </div>
                      <button
                        className="text-button"
                        disabled={busy}
                        onClick={() =>
                          run(
                            () => window.space.openInstance(profile.id),
                            () => setNotice('Папка профиля открыта.'),
                          )
                        }
                      >
                        <Icon kind="folder" /> Открыть папку игры
                      </button>
                    </section>
                  </div>
                )}
                {view === 'settings' && (
                  <div className="settings-grid">
                    <section className="panel">
                      <span className="eyebrow">ПРОВЕРКА СРЕДЫ</span>
                      <h2>На одной частоте.</h2>
                      <p className="muted">
                        Проверим Java выбранного профиля перед установкой игры.
                      </p>
                      <div className="diagnostic-row">
                        <span>Профиль</span>
                        <strong>{profile.name}</strong>
                      </div>
                      <div className="diagnostic-row">
                        <span>Память устройства</span>
                        <strong>{gb(snapshot.totalMemoryMb)}</strong>
                      </div>
                      <div className="diagnostic-row">
                        <span>Выделено игре</span>
                        <strong>{gb(profile.memoryMb)}</strong>
                      </div>
                      <div className="diagnostic-row">
                        <span>Java</span>
                        <strong>
                          {java[profile.id]?.usable
                            ? 'Проверена'
                            : profile.javaPath
                              ? 'Не проверена'
                              : 'Не выбрана'}
                        </strong>
                      </div>
                      <button
                        className="primary"
                        disabled={busy || !profile.javaPath}
                        onClick={checkJava}
                      >
                        Проверить Java <Icon kind="arrow" />
                      </button>
                      {!profile.javaPath && (
                        <p className="inline-hint">
                          Сначала выберите java.exe в разделе «Профили».
                        </p>
                      )}
                      {java[profile.id] && (
                        <p className="java-result" role="status">
                          {java[profile.id].message}
                        </p>
                      )}
                    </section>
                    <section className="panel storage-card">
                      <Orbit />
                      <h2>Всё на своём месте.</h2>
                      <p className="muted">
                        Настройки лаунчера хранятся локально. У каждого профиля будет отдельная
                        папка игры.
                      </p>
                      <label>Папка данных</label>
                      <p className="file-path">{snapshot.dataRoot}</p>
                      <button
                        className="secondary"
                        disabled={busy}
                        onClick={() =>
                          run(
                            () => window.space.openInstance(profile.id),
                            () => setNotice('Папка профиля открыта.'),
                          )
                        }
                      >
                        Папка текущего профиля
                      </button>
                      <p className="inline-hint">
                        Лаунчер не использует папку .minecraft других установок.
                      </p>
                    </section>
                  </div>
                )}
              </>
            )
          )}
          <footer className="app-footer">
            <span>
              <span className="status-dot" /> Настройки на этом устройстве
            </span>
            <span>SPACE CLIENT · СОБСТВЕННАЯ ОРБИТА</span>
          </footer>
        </main>
      </div>
    </>
  );
}
createRoot(document.getElementById('root')!).render(<App />);
