import { useEffect, useRef, useState } from 'react';
import {
  CircleHelp,
  LayoutDashboard,
  Layers3,
  LoaderCircle,
  Settings2,
  ShieldCheck,
  UserRound,
} from 'lucide-react';
import { Button } from './components/ui/button';
import { Badge } from './components/ui/badge';
import { Card, CardContent } from './components/ui/card';
import { SpaceMark } from './components/space-mark';
import { useLauncherSession } from './hooks/use-launcher-session';
import { HomePage } from './pages/home';
import { ProfilesPage } from './pages/profiles';
import { SettingsPage } from './pages/settings';
type View = 'home' | 'profiles' | 'settings';
const navigation = [
  { id: 'home', label: 'Обзор', icon: LayoutDashboard },
  { id: 'profiles', label: 'Профили', icon: Layers3 },
  { id: 'settings', label: 'Настройки', icon: Settings2 },
] as const;
const titles = {
  home: ['Играть', 'Твоя следующая орбита начинается здесь.'],
  profiles: ['Профили игры', 'Отдельные настройки для каждого твоего мира.'],
  settings: ['Настройки лаунчера', 'Java, память и файлы на твоём устройстве.'],
};

export function App() {
  const [view, setView] = useState<View>('home');
  const model = useLauncherSession();
  const { snapshot, profile, draft, busy, dirty, error, notice, session } = model;
  const ready = snapshot && profile && draft ? { ...model, snapshot, profile, draft } : null;
  const heading = useRef<HTMLHeadingElement>(null);
  const main = useRef<HTMLElement>(null);
  const previousView = useRef<View>('home');
  const load = session.load;
  const changeView = (next: View) => {
    if (next === view) return;
    setView(next);
    session.dismissFeedback();
  };
  useEffect(() => {
    if (previousView.current === view) return;
    previousView.current = view;
    if (main.current) main.current.scrollTop = 0;
    heading.current?.focus({ preventScroll: true });
  }, [view]);
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
              SPACE<span>JAVA CLIENT</span>
            </div>
          </div>
          <p className="nav-caption">НАВИГАЦИЯ</p>
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
                <strong>{snapshot?.localNickname ?? 'Локальный игрок'}</strong>
                <span>Локальный режим</span>
              </div>
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
                <p className="page-eyebrow">SPACE CLIENT / ЛАУНЧЕР</p>
                <h1 ref={heading} tabIndex={-1}>
                  {titles[view][0]}
                </h1>
                <p>{titles[view][1]}</p>
              </div>
              <Badge variant="outline" className="edition-badge">
                <span className="status-dot" aria-hidden="true" />
                ECLIPSE / 01
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
              ready && (
                <div className="view-content" key={view}>
                  {view === 'home' && (
                    <HomePage model={ready} onProfiles={() => changeView('profiles')} />
                  )}
                  {view === 'profiles' && <ProfilesPage model={ready} />}
                  {view === 'settings' && <SettingsPage model={ready} />}
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
