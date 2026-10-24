import { EclipseScene } from '../components/eclipse-scene';
import { InstallationCard } from '../installation-card';
import { LaunchCard } from '../launch-card';
import { useInstallation } from '../hooks/use-installation';
import type { ReadyLauncherModel } from '../model/launcher-session';
export function HomePage({
  model,
  onProfiles,
}: {
  model: ReadyLauncherModel;
  onProfiles: () => void;
}) {
  const installation = useInstallation(model.profile.id);
  const current = model.launch?.profileId === model.profile.id ? model.launch : null;
  const launching = !!current && ['checking', 'starting', 'running'].includes(current.phase);
  return (
    <>
      <section className="launch-hero" aria-label="Space Client для Minecraft Java">
        <div className="hero-art">
          <EclipseScene />
        </div>
        <div className="hero-content">
          <p className="hero-kicker">SPACE CLIENT / MINECRAFT НА СВОЕЙ ОРБИТЕ</p>
          <h2>
            Твой мир.
            <br />
            <span>Новая орбита.</span>
          </h2>
          <p className="hero-description">Знакомый мир. Твой взгляд на игру.</p>
        </div>
        <span className="hero-caption">ECLIPSE / 01</span>
      </section>
      <div className="play-workspace">
        <LaunchCard model={model} onProfiles={onProfiles} preparing={installation.running} />
        <InstallationCard
          installation={installation}
          locked={launching || model.busy || model.dirty || model.nicknameDirty}
        />
      </div>
    </>
  );
}
