import Link from 'next/link';
import { PageIntro } from '@/shared/ui/page-intro';
import ContentError from '@/shared/ui/content-error';
import CopyHash from './copy-hash';
import { Arrow } from '@/shared/ui/icons';
import { getReleases } from '@/shared/api/index.server';
import { recommendedRelease } from '@/entities/release';
import { pageMetadata } from '@/shared/lib/metadata';
export async function generateMetadata() {
  const result = await getReleases();
  return pageMetadata(
    '/download',
    'Загрузки — Space Client',
    'Проверенные сборки Space Client, совместимость, версии и контрольные суммы.',
    !result.ok,
  );
}
export default async function Download({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const params = await searchParams;
  const channel =
    typeof params.channel === 'string' && ['alpha', 'beta', 'stable'].includes(params.channel)
      ? params.channel
      : '';
  const result = await getReleases();
  const recommended = result.ok ? recommendedRelease(result.data.items) : undefined;
  const items = result.ok
    ? [...result.data.items]
        .filter((x) => !channel || x.channel === channel)
        .sort(
          (a, b) =>
            Number(b.id === recommended?.id) - Number(a.id === recommended?.id) ||
            b.publishedAt.localeCompare(a.publishedAt) ||
            a.id.localeCompare(b.id),
        )
    : [];
  return (
    <div className="container page-content">
      <PageIntro
        label="ГОТОВНОСТЬ К СТАРТУ"
        title={recommended ? 'Твоя следующая орбита.' : 'Готовим новую орбиту.'}
      >
        Проверенные сборки Space Client и всё необходимое для установки.
      </PageIntro>
      <div className="two-columns">
        <section>
          {!result.ok ? (
            <ContentError title="Не удалось загрузить каталог." />
          ) : result.data.total === 0 ? (
            <div className="page-panel">
              <span className="status">В РАЗРАБОТКЕ</span>
              <h2 className="panel-title">Первый релиз ещё впереди.</h2>
              <p>
                Пользовательский установщик пока не выпущен. Dev-мод не заменяет готовый клиент.
              </p>
              <div className="actions">
                <Link className="button secondary" href="/updates">
                  Следить за обновлениями <Arrow />
                </Link>
                <Link className="text-link" href="/help">
                  Помощь →
                </Link>
              </div>
            </div>
          ) : (
            <>
              <form key={channel} className="filter-form" action="/download">
                <label>
                  Канал
                  <select name="channel" defaultValue={channel}>
                    <option value="">Все каналы</option>
                    <option value="stable">Stable</option>
                    <option value="beta">Beta</option>
                    <option value="alpha">Alpha</option>
                  </select>
                </label>
                <button className="button">Показать</button>
              </form>
              <div className="release-list">
                {items.map((release) => (
                  <article className="page-panel" key={release.id}>
                    <p className="eyebrow">
                      {release.channel.toUpperCase()} ·{' '}
                      <time dateTime={release.publishedAt}>
                        {new Date(release.publishedAt).toLocaleDateString('ru-RU', {
                          timeZone: 'UTC',
                        })}
                      </time>
                    </p>
                    <h2>Space Client {release.version}</h2>
                    {release.withdrawn ? (
                      <p className="release-notice">Сборка отозвана. Скачивание недоступно.</p>
                    ) : (
                      <>
                        {release.id === recommended?.id && (
                          <p className="status">Рекомендуемая сборка</p>
                        )}
                        {release.channel !== 'stable' && (
                          <p className="release-notice">
                            Предварительная версия. Возможны ошибки и несовместимости.
                          </p>
                        )}
                      </>
                    )}
                    <p>Minecraft {release.minecraftVersions.join(', ')}</p>
                    {release.artifacts.map((artifact) => (
                      <div
                        className="artifact"
                        key={`${artifact.platform}-${artifact.architecture}`}
                      >
                        <p>
                          Windows x64 · {artifact.fileName.split('.').at(-1)?.toUpperCase()} ·{' '}
                          {(artifact.sizeBytes / 1024 / 1024).toLocaleString('ru-RU', {
                            maximumFractionDigits: 1,
                          })}{' '}
                          МБ
                        </p>
                        <p className="caption">
                          Подпись:{' '}
                          {artifact.signatureStatus === 'signed'
                            ? 'подписан'
                            : artifact.signatureStatus === 'unsigned'
                              ? 'не подписан'
                              : 'статус не подтверждён'}
                          . SHA-256 проверяет целостность файла и не заменяет подпись.
                        </p>
                        {!release.withdrawn && (
                          <a className="button" href={artifact.downloadUrl}>
                            Скачать для Windows <Arrow />
                          </a>
                        )}
                        <CopyHash hash={artifact.sha256} />
                      </div>
                    ))}
                    <Link className="text-link" href={`/updates/${release.changelogSlug}`}>
                      Список изменений →
                    </Link>
                  </article>
                ))}
              </div>
              {!items.length && (
                <div className="empty-state">
                  <h2>В этом канале сборок нет.</h2>
                  <Link className="text-link" href="/download">
                    Показать все →
                  </Link>
                </div>
              )}
            </>
          )}
        </section>
        <aside className="page-panel compatibility-panel">
          <p className="eyebrow">ЦЕЛИ ПЕРВОЙ ВЕРСИИ</p>
          <h2>Оставайся в контексте.</h2>
          <dl>
            <dt>Игра</dt>
            <dd>Minecraft Java 1.8.9</dd>
            <dt>Система</dt>
            <dd>Windows x64</dd>
            <dt>Статус</dt>
            <dd>Разработка</dd>
          </dl>
          <p>
            Требования к памяти, диску и Java уточним перед alpha. Для macOS и Linux сборок пока
            нет.
          </p>
          <Link className="text-link" href="/help/compatibility">
            О совместимости →
          </Link>
        </aside>
      </div>
    </div>
  );
}
