import type { SpaceBuildIdentity, VerifiedSpaceBuild } from '../../shared/build-identity';
import { developmentBuildId } from '../../shared/build-identity';

function BuildValues({ build }: { build: SpaceBuildIdentity }) {
  return (
    <dl className="build-values">
      <div>
        <dt>ID исходников</dt>
        <dd>
          <code>{developmentBuildId(build)}</code>
        </dd>
      </div>
      <div>
        <dt>Game source tree</dt>
        <dd>
          <code>{build.gameSourceTree}</code>
        </dd>
      </div>
      <div>
        <dt>SHA-256 JAR</dt>
        <dd>
          <code>{build.sha256}</code>
        </dd>
      </div>
      <div>
        <dt>Размер JAR</dt>
        <dd>{build.size.toLocaleString('ru-RU')} байт</dd>
      </div>
      <div>
        <dt>Версии</dt>
        <dd>
          Space {build.modVersion} · Minecraft {build.minecraftVersion} · Fabric{' '}
          {build.loaderVersion}
        </dd>
      </div>
    </dl>
  );
}

export function BuildIdentity({
  bundled,
  verified,
}: {
  bundled: SpaceBuildIdentity | null;
  verified: VerifiedSpaceBuild | null;
}) {
  const sameBytes =
    !!bundled &&
    !!verified &&
    bundled.sha256 === verified.identity.sha256 &&
    bundled.size === verified.identity.size &&
    bundled.gameSourceTree === verified.identity.gameSourceTree;
  return (
    <section className="build-identity" aria-label="Идентичность игровой сборки">
      <p className="build-label">
        Сборка в лаунчере <span>DEVELOPMENT</span>
      </p>
      <p className="build-id">{bundled ? developmentBuildId(bundled) : 'Space не включён'}</p>
      <p className="build-verification" role="status" aria-live="polite" aria-atomic="true">
        {verified
          ? `Последняя проверка перед запуском: ${developmentBuildId(verified.identity)}. ${sameBytes ? 'JAR совпал со сборкой лаунчера.' : 'JAR отличается от сборки лаунчера.'}`
          : bundled
            ? 'Установленный Space ещё не проверен перед запуском в этом сеансе.'
            : 'Для локальной игры нужен лаунчер со сборкой Space. Здесь можно подготовить только базовые файлы Minecraft.'}
      </p>
      {bundled && (
        <details className="build-details">
          <summary>Подробности сборки</summary>
          <p className="build-detail-heading">В составе лаунчера</p>
          <BuildValues build={bundled} />
          {verified && (
            <>
              <p className="build-detail-heading">Установленный JAR при последней проверке</p>
              <BuildValues build={verified.identity} />
              <p className="inline-hint">
                Проверено:{' '}
                <time dateTime={verified.checkedAt}>
                  {new Date(verified.checkedAt).toLocaleString('ru-RU')}
                </time>
              </p>
            </>
          )}
          <p className="inline-hint">
            ID отмечает исходники, SHA-256 — точные байты JAR. Файлы хранятся в отдельной папке
            профиля; при каждом запуске они и Java проверяются заново.
          </p>
        </details>
      )}
    </section>
  );
}
