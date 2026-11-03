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
  const nicknameReady = !!model.snapshot.localNickname && !model.nicknameDirty;
  const lockReason = launching
    ? 'Файлы можно подготовить после завершения игры или запуска.'
    : model.busy
      ? 'Дождитесь завершения текущего действия.'
      : model.dirty
        ? 'Сохраните или отмените изменения в разделе «Профили».'
        : model.nicknameDirty
          ? 'Сначала сохраните ник в карточке профиля.'
          : '';
  return (
    <>
      <section className="launch-hero" aria-label="Space Client для Minecraft Java">
        <div className="hero-art">
          <EclipseScene />
        </div>
        <div className="hero-content">
          <p className="hero-kicker">MINECRAFT JAVA 1.8.9 / ЛОКАЛЬНАЯ ИГРА</p>
          <h2>
            Твой мир.
            <br />
            <span>Новая орбита.</span>
          </h2>
        </div>
        <ol className="launch-steps" aria-label="Подготовка к локальной игре">
          <li data-complete={nicknameReady}>
            <span className="step-number" aria-hidden="true">
              01
            </span>
            <div>
              <strong>Ник</strong>
              <span>{nicknameReady ? model.snapshot.localNickname : 'Введите и сохраните'}</span>
            </div>
          </li>
          <li data-complete={installation.prepared && !installation.error}>
            <span className="step-number" aria-hidden="true">
              02
            </span>
            <div>
              <strong>Файлы</strong>
              <span>
                {installation.running
                  ? 'Подготовка…'
                  : installation.error || installation.current?.phase === 'error'
                    ? 'Нужна проверка'
                    : !installation.current
                      ? 'Проверяем…'
                      : installation.prepared
                        ? 'Подготовлены'
                        : 'Подготовьте ниже'}
              </span>
            </div>
          </li>
          <li>
            <span className="step-number" aria-hidden="true">
              03
            </span>
            <div>
              <strong>Запуск</strong>
              <span>
                {current?.phase === 'running'
                  ? 'Процесс работает'
                  : launching
                    ? 'Запускаем…'
                    : current?.phase === 'error'
                      ? 'Ошибка · детали ниже'
                      : 'С проверкой Java'}
              </span>
            </div>
          </li>
        </ol>
      </section>
      <div className="play-workspace">
        <LaunchCard model={model} onProfiles={onProfiles} preparing={installation.running} />
        <InstallationCard installation={installation} launch={current} lockReason={lockReason} />
      </div>
    </>
  );
}
