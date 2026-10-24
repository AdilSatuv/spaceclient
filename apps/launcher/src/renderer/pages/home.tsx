import { ArrowRight, Cpu, Layers3, ShieldCheck, Terminal } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { SpaceMark } from '../components/space-mark';
import { InstallationCard } from '../installation-card';
import { LaunchCard } from '../launch-card';
import { gb } from '../lib/format';
import type { ReadyLauncherModel } from '../model/launcher-session';
export function HomePage({
  model,
  onProfiles,
}: {
  model: ReadyLauncherModel;
  onProfiles: () => void;
}) {
  const { profile, dirty, javaLabel } = model;
  return (
    <>
      <LaunchCard model={model} />
      <div className="home-feature-grid">
        <section className="launch-hero" aria-label="Space Client для Minecraft Java">
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
            <Button variant="outline" className="w-full" onClick={onProfiles}>
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
          Отдельная папка для каждого профиля. Твои другие установки Minecraft остаются на месте.
        </span>
      </div>
    </>
  );
}
