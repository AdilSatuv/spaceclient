import { useEffect, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import {
  ArrowRight,
  Check,
  ChevronRight,
  CircleHelp,
  Cpu,
  FolderOpen,
  HardDrive,
  LayoutDashboard,
  Layers3,
  LoaderCircle,
  LockKeyhole,
  Monitor,
  Plus,
  Settings2,
  ShieldCheck,
  Terminal,
  UserRound,
} from 'lucide-react';
import type { JavaInfo, LauncherApi, ProfileEdit, Result, Snapshot } from '../shared/contracts';
import { Button } from './components/ui/button';
import { Badge } from './components/ui/badge';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from './components/ui/card';
import { Input } from './components/ui/input';
import { cn } from './lib/utils';
import { InstallationCard } from './installation-card';

declare global {
  interface Window {
    space: LauncherApi;
  }
}
type View = 'home' | 'profiles' | 'settings';
const navigation = [
  { id: 'home', label: 'Обзор', icon: LayoutDashboard },
  { id: 'profiles', label: 'Профили', icon: Layers3 },
  { id: 'settings', label: 'Настройки', icon: Settings2 },
] as const;
const titles = {
  home: ['Твоя следующая орбита.', 'Всё для игры — в одном месте.'],
  profiles: ['Профили игры', 'Отдельные настройки для каждого твоего мира.'],
  settings: ['Настройки лаунчера', 'Java, память и файлы на твоём устройстве.'],
};
const gb = (mb: number) =>
  new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 2 }).format(mb / 1024) + ' ГБ';
function SpaceMark({ className }: { className?: string }) {
  return (
    <svg className={cn('space-mark', className)} viewBox="0 0 40 40" fill="none" aria-hidden="true">
      <path d="m20 6 4 10 10 4-10 4-4 10-4-10-10-4 10-4Z" fill="currentColor" />
      <ellipse
        cx="20"
        cy="20"
        rx="20"
        ry="7"
        transform="rotate(-38 20 20)"
        stroke="currentColor"
        strokeWidth="1.4"
      />
    </svg>
  );
}
function App() {
  const [snapshot, setSnapshot] = useState<Snapshot | null>(null);
  const [view, setView] = useState<View>('home');
  const [draft, setDraft] = useState<ProfileEdit | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [java, setJava] = useState<Record<string, JavaInfo>>({});
  const heading = useRef<HTMLHeadingElement>(null);
  const main = useRef<HTMLElement>(null);
  const previousView = useRef<View>('home');
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
    if (next === view) return;
    setView(next);
    setError('');
    setNotice('');
  };
  useEffect(() => {
    if (previousView.current === view) return;
    previousView.current = view;
    if (main.current) main.current.scrollTop = 0;
    heading.current?.focus({ preventScroll: true });
  }, [view]);
  const checkJava = () =>
    profile &&
    run(
      () => window.space.checkJava(profile.id),
      (result) => {
        setJava((current) => ({ ...current, [profile.id]: result }));
        setNotice(result.message);
      },
    );
  const openFolder = () =>
    profile &&
    run(
      () => window.space.openInstance(profile.id),
      () => setNotice('Папка профиля открыта.'),
    );
  const chooseJava = () =>
    profile &&
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
    );
  const javaLabel =
    profile &&
    (java[profile.id]?.usable
      ? 'Проверена · 21 x64'
      : profile.javaPath
        ? 'Нужна проверка'
        : 'Не выбрана');

  return (
    <>
      <Button className="skip-link" onClick={() => main.current?.focus()}>
        К содержимому
      </Button>
      <div className="titlebar">
        <SpaceMark />
        <span>SPACE CLIENT</span>
        <span className="title-version">Лаунчер</span>
      </div>
      <div className="app-shell">
        <aside className="sidebar">
          <div className="brand">
            <span className="brand-mark">
              <SpaceMark />
            </span>
            <div className="brand-copy">
              space<span>Игровой клиент</span>
            </div>
          </div>
          <p className="nav-caption">ПРОСТРАНСТВО</p>
          <nav aria-label="Разделы лаунчера">
            {navigation.map(({ id, label, icon: Icon }) => (
              <Button
                key={id}
                variant="ghost"
                className="nav-item"
                aria-label={label}
                aria-current={view === id ? 'page' : undefined}
                onClick={() => changeView(id)}
              >
                <Icon aria-hidden="true" />
                <span className="nav-text">{label}</span>
                {id === 'profiles' && dirty && (
                  <span className="draft-dot" aria-label="Есть несохранённые изменения" />
                )}
              </Button>
            ))}
          </nav>
          <div className="sidebar-bottom">
            <div className="account-summary">
              <span className="account-avatar">
                <UserRound aria-hidden="true" />
              </span>
              <div className="account-copy">
                <strong>Твой аккаунт</strong>
                <span>Microsoft не подключён</span>
              </div>
              <LockKeyhole className="account-lock" aria-hidden="true" />
            </div>
            <div className="build-caption">
              <span className="status-dot" />
              Development · 0.1.0
            </div>
          </div>
        </aside>
        <main id="main" ref={main} tabIndex={-1} aria-busy={busy}>
          <div className="main-content">
            <header className="page-header">
              <div>
                <div className="breadcrumb">
                  Space Client <ChevronRight aria-hidden="true" />
                  {navigation.find((item) => item.id === view)?.label}
                </div>
                <h1 ref={heading} tabIndex={-1}>
                  {titles[view][0]}
                </h1>
                <p>{titles[view][1]}</p>
              </div>
              <Badge variant="outline" className="edition-badge">
                <Monitor aria-hidden="true" />
                Windows x64
              </Badge>
            </header>
            {error && (
              <div className="feedback error" role="alert">
                <CircleHelp aria-hidden="true" />
                {error}
              </div>
            )}
            <div className="feedback-slot" role="status" aria-live="polite">
              {busy ? (
                <>
                  <LoaderCircle className="spinner" aria-hidden="true" />
                  Выполняем действие…
                </>
              ) : (
                notice
              )}
            </div>
            {snapshot?.warning && <div className="feedback warning">{snapshot.warning}</div>}
            {!snapshot ? (
              <Card className="startup">
                <CardContent>
                  <SpaceMark />
                  <h2>{busy ? 'Загружаем твой профиль…' : 'Не удалось загрузить профиль'}</h2>
                  <p>Настройки хранятся на этом устройстве.</p>
                  {!busy && <Button onClick={load}>Повторить</Button>}
                </CardContent>
              </Card>
            ) : (
              profile &&
              draft && (
                <div className="view-content" key={view}>
                  {view === 'home' && (
                    <>
                      <div className="home-feature-grid">
                        <section
                          className="launch-hero"
                          aria-label="Space Client для Minecraft Java"
                        >
                          <div className="hero-art" aria-hidden="true" />
                          <div className="hero-content">
                            <Badge variant="outline" className="hero-badge">
                              <SpaceMark />
                              JAVA EDITION · 1.8.9
                            </Badge>
                            <div>
                              <p className="hero-kicker">ЗНАКОМЫЙ МИР. НОВАЯ АТМОСФЕРА.</p>
                              <h2>
                                За пределами
                                <br />
                                <span>обычной игры.</span>
                              </h2>
                              <p className="hero-description">
                                Твои настройки. Твой ритм.
                                <br />
                                Твой Minecraft со Space Client.
                              </p>
                            </div>
                          </div>
                          <span className="hero-caption" aria-hidden="true">
                            SPACE / 001
                          </span>
                        </section>
                        <Card className="current-profile">
                          <CardHeader>
                            <div className="card-eyebrow">
                              АКТИВНЫЙ ПРОФИЛЬ
                              <Layers3 aria-hidden="true" />
                            </div>
                            <CardTitle>
                              <h2>{profile.name}</h2>
                            </CardTitle>
                            <CardDescription>Готовим всё к твоей игре</CardDescription>
                          </CardHeader>
                          <CardContent>
                            <dl className="profile-facts">
                              <div>
                                <dt>
                                  <Cpu aria-hidden="true" />
                                  Память
                                </dt>
                                <dd>{gb(profile.memoryMb)}</dd>
                              </div>
                              <div>
                                <dt>
                                  <Terminal aria-hidden="true" />
                                  Java
                                </dt>
                                <dd>{javaLabel}</dd>
                              </div>
                            </dl>
                            <Button
                              variant="outline"
                              className="w-full"
                              onClick={() => changeView('profiles')}
                            >
                              Управлять профилем
                              <ArrowRight aria-hidden="true" />
                            </Button>
                          </CardContent>
                        </Card>
                      </div>
                      <InstallationCard profileId={profile.id} dirty={dirty} />
                      <div className="home-note">
                        <ShieldCheck aria-hidden="true" />
                        <span>
                          Отдельная папка для каждого профиля. Твои другие установки Minecraft
                          остаются на месте.
                        </span>
                      </div>
                    </>
                  )}
                  {view === 'profiles' && (
                    <div className="profiles-grid">
                      <Card className="profile-list">
                        <CardHeader>
                          <div className="section-heading">
                            <CardTitle>
                              <h2>Твои профили</h2>
                            </CardTitle>
                            <Badge variant="secondary">{snapshot.profiles.length} / 10</Badge>
                          </div>
                          <CardDescription>Свои настройки для каждой игры.</CardDescription>
                        </CardHeader>
                        <CardContent>
                          <div className="profile-options">
                            {snapshot.profiles.map((item) => (
                              <Button
                                variant="ghost"
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
                                  <SpaceMark />
                                </span>
                                <span className="profile-option-copy">
                                  <strong>{item.name}</strong>
                                  <small>{gb(item.memoryMb)} · 1.8.9</small>
                                </span>
                                {item.id === profile.id && (
                                  <Check className="selected-mark" aria-hidden="true" />
                                )}
                              </Button>
                            ))}
                          </div>
                          <Button
                            variant="outline"
                            className="w-full create-profile"
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
                            <Plus aria-hidden="true" />
                            Создать профиль
                          </Button>
                          {dirty && (
                            <p className="inline-hint">
                              Сохраните или отмените изменения перед сменой профиля.
                            </p>
                          )}
                        </CardContent>
                      </Card>
                      <Card className="profile-form">
                        <CardHeader>
                          <div className="section-heading">
                            <CardTitle>
                              <h2>Настройки профиля</h2>
                            </CardTitle>
                            <Badge variant="outline">Локально</Badge>
                          </div>
                          <CardDescription>Изменения применяются после сохранения.</CardDescription>
                        </CardHeader>
                        <CardContent>
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
                            <div className="form-field">
                              <label htmlFor="profile-name">Название профиля</label>
                              <Input
                                id="profile-name"
                                maxLength={40}
                                required
                                disabled={busy}
                                value={draft.name}
                                aria-describedby="name-hint"
                                onChange={(event) =>
                                  setDraft({ ...draft, name: event.target.value })
                                }
                              />
                              <p id="name-hint" className="inline-hint">
                                До 40 символов. Например, «Основной» или «PvP».
                              </p>
                            </div>
                            <div className="form-field">
                              <div className="range-heading">
                                <label htmlFor="memory">Память для игры</label>
                                <output htmlFor="memory" className="memory-value">
                                  {gb(draft.memoryMb)}
                                </output>
                              </div>
                              <input
                                id="memory"
                                className="memory-range"
                                type="range"
                                min={1024}
                                max={snapshot.maxMemoryMb}
                                step={256}
                                disabled={busy}
                                value={draft.memoryMb}
                                aria-valuetext={gb(draft.memoryMb)}
                                aria-describedby="memory-hint"
                                onChange={(event) =>
                                  setDraft({ ...draft, memoryMb: Number(event.target.value) })
                                }
                              />
                              <div className="range-limits">
                                <span>1 ГБ</span>
                                <span>{gb(snapshot.maxMemoryMb)}</span>
                              </div>
                              <p id="memory-hint" className="inline-hint">
                                Всего {gb(snapshot.totalMemoryMb)}. Часть памяти оставляем системе.
                              </p>
                            </div>
                            <div className="form-actions">
                              <Button type="submit" disabled={busy || !dirty || !draft.name.trim()}>
                                Сохранить изменения
                              </Button>
                              <Button
                                type="button"
                                variant="outline"
                                disabled={busy || !dirty}
                                onClick={() => {
                                  apply(snapshot);
                                  setError('');
                                  setNotice('Изменения отменены.');
                                }}
                              >
                                Отменить
                              </Button>
                            </div>
                          </form>
                          <section className="java-section" aria-labelledby="java-heading">
                            <div className="section-heading">
                              <h3 id="java-heading">
                                <Terminal aria-hidden="true" />
                                Java для этого профиля
                              </h3>
                              <Badge variant="secondary">21 · x64</Badge>
                            </div>
                            <p className="file-path">
                              {profile.javaPath || 'Файл java.exe ещё не выбран'}
                            </p>
                            <div className="form-actions">
                              <Button
                                variant="outline"
                                disabled={busy || dirty}
                                onClick={chooseJava}
                              >
                                <FolderOpen aria-hidden="true" />
                                Выбрать Java
                              </Button>
                              <Button
                                variant="secondary"
                                disabled={busy || dirty || !profile.javaPath}
                                onClick={checkJava}
                              >
                                Проверить Java
                              </Button>
                            </div>
                            {java[profile.id] && (
                              <p
                                className={cn('java-result', java[profile.id].usable && 'success')}
                                role="status"
                              >
                                {java[profile.id].message}
                              </p>
                            )}
                          </section>
                          <Button
                            variant="ghost"
                            className="folder-action"
                            disabled={busy}
                            onClick={openFolder}
                          >
                            <FolderOpen aria-hidden="true" />
                            Открыть папку игры
                            <ArrowRight aria-hidden="true" />
                          </Button>
                        </CardContent>
                      </Card>
                    </div>
                  )}
                  {view === 'settings' && (
                    <>
                      <div className="settings-grid">
                        <Card>
                          <CardHeader>
                            <span className="section-icon">
                              <Terminal aria-hidden="true" />
                            </span>
                            <CardTitle>
                              <h2>Среда для игры</h2>
                            </CardTitle>
                            <CardDescription>Проверка Java в активном профиле.</CardDescription>
                          </CardHeader>
                          <CardContent>
                            <dl className="diagnostic-list">
                              <div>
                                <dt>Профиль</dt>
                                <dd>{profile.name}</dd>
                              </div>
                              <div>
                                <dt>Память устройства</dt>
                                <dd>{gb(snapshot.totalMemoryMb)}</dd>
                              </div>
                              <div>
                                <dt>Выделено игре</dt>
                                <dd>{gb(profile.memoryMb)}</dd>
                              </div>
                              <div>
                                <dt>Java</dt>
                                <dd>{javaLabel}</dd>
                              </div>
                            </dl>
                            <Button disabled={busy || !profile.javaPath} onClick={checkJava}>
                              <ShieldCheck aria-hidden="true" />
                              Проверить Java
                            </Button>
                            {!profile.javaPath && (
                              <p className="inline-hint">
                                Сначала выберите java.exe в разделе «Профили».
                              </p>
                            )}
                            {java[profile.id] && (
                              <p
                                className={cn('java-result', java[profile.id].usable && 'success')}
                                role="status"
                              >
                                {java[profile.id].message}
                              </p>
                            )}
                          </CardContent>
                        </Card>
                        <Card>
                          <CardHeader>
                            <span className="section-icon">
                              <HardDrive aria-hidden="true" />
                            </span>
                            <CardTitle>
                              <h2>Хранилище</h2>
                            </CardTitle>
                            <CardDescription>
                              Настройки и игровые файлы на устройстве.
                            </CardDescription>
                          </CardHeader>
                          <CardContent>
                            <p className="field-caption">Папка данных лаунчера</p>
                            <p className="file-path storage-path">{snapshot.dataRoot}</p>
                            <Button variant="outline" disabled={busy} onClick={openFolder}>
                              <FolderOpen aria-hidden="true" />
                              Папка текущего профиля
                            </Button>
                            <p className="inline-hint storage-hint">
                              Каждый профиль получает отдельную папку. Лаунчер не использует
                              .minecraft других установок.
                            </p>
                          </CardContent>
                        </Card>
                      </div>
                      <Card className="account-notice">
                        <CardContent>
                          <span className="section-icon">
                            <LockKeyhole aria-hidden="true" />
                          </span>
                          <div>
                            <h2>Microsoft-аккаунт</h2>
                            <p>
                              Вход и проверка владения Minecraft ещё в разработке. Запуск игры пока
                              недоступен.
                            </p>
                          </div>
                          <Badge variant="outline">В разработке</Badge>
                        </CardContent>
                      </Card>
                    </>
                  )}
                </div>
              )
            )}
            <footer className="app-footer">
              <span>
                <ShieldCheck aria-hidden="true" />
                Настройки на этом устройстве
              </span>
              <span>
                SPACE CLIENT <span className="footer-separator">/</span> 0.1.0
              </span>
            </footer>
          </div>
        </main>
      </div>
    </>
  );
}
createRoot(document.getElementById('root')!).render(<App />);
