export type FeatureStatus = 'planned' | 'prototype' | 'verified' | 'released';
export type Feature = { id: string; slug: string; name: string; summary: string; category: 'hud' | 'comfort' | 'launcher' | 'performance'; status: FeatureStatus; details: string; releaseId?: string };
export type Artifact = { platform: 'windows'; architecture: 'x64'; fileName: string; sizeBytes: number; sha256: string; downloadUrl: string; signatureStatus: 'signed' | 'unsigned' | 'unknown' };
export type Release = { id: string; version: string; channel: 'alpha' | 'beta' | 'stable'; publishedAt: string; minecraftVersions: string[]; changelogSlug: string; artifacts: Artifact[]; withdrawn?: boolean };
export type Update = { id: string; slug: string; type: 'development' | 'release'; title: string; summary: string; publishedAt: string; blocks: { heading: string; text: string }[]; releaseId?: string };

export const features: Feature[] = [
  { id: 'fps', slug: 'fps', name: 'FPS Counter', summary: 'Частота кадров прямо в игре.', category: 'hud', status: 'prototype', details: 'Показатель берётся из Minecraft. Проверка в мире ещё предстоит.' },
  { id: 'coordinates', slug: 'coordinates', name: 'Координаты', summary: 'XYZ без отладочного экрана.', category: 'hud', status: 'prototype', details: 'В dev-моде реализован виджет положения игрока.' },
  { id: 'keys', slug: 'keystrokes', name: 'Keystrokes', summary: 'Состояния клавиш движения перед глазами.', category: 'hud', status: 'prototype', details: 'Использует игровые bindings; полноценный редактор ещё в планах.' },
  { id: 'cps', slug: 'cps', name: 'CPS Counter', summary: 'Отдельный счётчик левой и правой кнопок.', category: 'hud', status: 'planned', details: 'Планируется скользящее окно в одну секунду; модуль ещё не завершён.' },
  { id: 'editor', slug: 'hud-editor', name: 'Редактор HUD', summary: 'Перемещение, масштаб и настройки виджетов.', category: 'comfort', status: 'planned', details: 'Цель — менять расположение модулей из самой игры.' },
  { id: 'profiles', slug: 'launch-profiles', name: 'Профили запуска', summary: 'Свои настройки памяти и игры.', category: 'launcher', status: 'planned', details: 'Пользовательского лаунчера пока нет.' }
];
export const releases: Release[] = [];
export const updates: Update[] = [{
  id: 'first-contact', slug: 'first-contact', type: 'development', title: 'Начинаем с фундамента.', summary: 'Первый Java-мод, исправление логирования и новый дизайн сайта в Figma.', publishedAt: '2026-10-23T00:00:00.000Z',
  blocks: [
    { heading: 'Первый игровой прототип', text: 'Подготовлена сборка Minecraft 1.8.9 на Legacy Fabric. В dev-моде есть FPS, XYZ, состояния клавиш и переключение HUD через F8. Проверка внутри мира ещё не завершена.' },
    { heading: 'Понятные журналы', text: 'Исправлена конфигурация Log4j dev-запуска. Ошибка старой Twitch-интеграции остаётся известным ограничением.' },
    { heading: 'Сначала дизайн', text: 'Публичный сайт использует Next.js и NestJS. Новый визуальный вариант создаётся в отдельном Figma Design-файле по подробному ТЗ.' },
    { heading: 'Что дальше', text: 'Пользовательского установщика пока нет. Дату alpha ещё не объявляем.' }
  ]
}];

const slugPattern = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;
export function validSlug(value: unknown): value is string { return typeof value === 'string' && value.length <= 100 && slugPattern.test(value); }
function requireValue(condition: unknown, message: string): asserts condition { if (!condition) throw new Error(message); }
function nonEmpty(value: unknown): value is string { return typeof value === 'string' && value.trim().length > 0; }
function timestamp(value: string) { return typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T/.test(value) && Number.isFinite(Date.parse(value)) && new Date(value).toISOString() === value; }
function unique(values: string[]) { return new Set(values).size === values.length; }
export function validateContent(content: { features: Feature[]; releases: Release[]; updates: Update[] }): void {
  requireValue(unique(content.features.map(x => x.id)) && unique(content.features.map(x => x.slug)), 'Duplicate feature id/slug');
  requireValue(unique(content.releases.map(x => x.id)), 'Duplicate release id');
  requireValue(unique(content.updates.map(x => x.slug)) && unique(content.updates.map(x => x.id)), 'Duplicate update id/slug');
  const releaseIds = new Set(content.releases.map(x => x.id));
  for (const feature of content.features) {
    requireValue(validSlug(feature.id) && validSlug(feature.slug) && nonEmpty(feature.name) && nonEmpty(feature.summary) && nonEmpty(feature.details), 'Invalid feature');
    requireValue(['hud', 'comfort', 'launcher', 'performance'].includes(feature.category), 'Invalid feature category');
    requireValue(['planned', 'prototype', 'verified', 'released'].includes(feature.status), 'Invalid feature status');
    requireValue(feature.status !== 'released' || (!!feature.releaseId && releaseIds.has(feature.releaseId)), 'Released feature requires release');
    requireValue(!feature.releaseId || releaseIds.has(feature.releaseId), 'Unknown feature release');
  }
  for (const update of content.updates) {
    requireValue(validSlug(update.id) && validSlug(update.slug) && nonEmpty(update.title) && nonEmpty(update.summary) && timestamp(update.publishedAt), 'Invalid update');
    requireValue(['development', 'release'].includes(update.type) && update.blocks.length > 0 && update.blocks.every(x => nonEmpty(x.heading) && nonEmpty(x.text)), 'Invalid update blocks/type');
    requireValue(update.type !== 'release' || (!!update.releaseId && releaseIds.has(update.releaseId)), 'Release update requires release');
    requireValue(!update.releaseId || releaseIds.has(update.releaseId), 'Unknown update release');
  }
  for (const release of content.releases) {
    requireValue(release.withdrawn === undefined || typeof release.withdrawn === 'boolean', 'Invalid withdrawn state');
    requireValue(validSlug(release.id) && /^\d+\.\d+\.\d+(?:-[a-z0-9.-]+)?$/.test(release.version), 'Invalid release identity');
    requireValue(['alpha', 'beta', 'stable'].includes(release.channel) && timestamp(release.publishedAt), 'Invalid release channel/date');
    requireValue(release.minecraftVersions.length > 0 && release.minecraftVersions.every(x => /^\d+\.\d+(?:\.\d+)?$/.test(x)), 'Invalid Minecraft versions');
    requireValue(content.updates.some(x => x.slug === release.changelogSlug && x.type === 'release' && x.releaseId === release.id), 'Missing release changelog');
    requireValue(release.artifacts.length > 0 && unique(release.artifacts.map(x => `${x.platform}/${x.architecture}`)), 'Missing/duplicate artifact');
    for (const artifact of release.artifacts) {
      requireValue(artifact.platform === 'windows' && artifact.architecture === 'x64', 'Unsupported platform');
      requireValue(/^[A-Za-z0-9][A-Za-z0-9._-]*\.(?:exe|msi|zip)$/.test(artifact.fileName) && Number.isSafeInteger(artifact.sizeBytes) && artifact.sizeBytes > 0 && /^[a-fA-F0-9]{64}$/.test(artifact.sha256), 'Invalid artifact metadata');
      requireValue(['signed', 'unsigned', 'unknown'].includes(artifact.signatureStatus), 'Invalid signature status');
      const url = new URL(artifact.downloadUrl);
      const segments = url.pathname.split('/');
      requireValue(url.protocol === 'https:' && url.hostname === 'github.com' && !url.port && !url.username && !url.password && !url.search && !url.hash && segments.length === 7 && segments[1] === 'AdilSatuv' && segments[2] === 'spaceclient' && segments[3] === 'releases' && segments[4] === 'download' && segments[5].length > 0 && decodeURIComponent(segments[6]) === artifact.fileName, 'Artifact URL outside release allowlist');
    }
  }
}
