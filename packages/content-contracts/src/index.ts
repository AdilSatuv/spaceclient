export type FeatureStatus = 'planned' | 'prototype' | 'verified' | 'released';
export type Feature = {
  id: string;
  slug: string;
  name: string;
  summary: string;
  details: string;
  category: 'hud' | 'comfort' | 'launcher' | 'performance';
  status: FeatureStatus;
  releaseId?: string;
};
export type Update = {
  id: string;
  slug: string;
  title: string;
  summary: string;
  type: 'development' | 'release';
  publishedAt: string;
  blocks: { heading: string; text: string }[];
  releaseId?: string;
};
export type Artifact = {
  platform: 'windows';
  architecture: 'x64';
  fileName: string;
  sizeBytes: number;
  sha256: string;
  downloadUrl: string;
  signatureStatus: 'signed' | 'unsigned' | 'unknown';
};
export type Release = {
  id: string;
  version: string;
  channel: 'alpha' | 'beta' | 'stable';
  publishedAt: string;
  minecraftVersions: string[];
  changelogSlug: string;
  artifacts: Artifact[];
  withdrawn?: boolean;
};
export type UpdateList = { items: Update[]; page: number; pageSize: number; total: number };
export type ContentCatalog = { features: Feature[]; releases: Release[]; updates: Update[] };
export const UPDATE_PAGE_SIZE = 12;
export type UpdateQuery = { type: 'all' | Update['type']; page: number };
export const validUpdateType = (value: unknown): value is UpdateQuery['type'] =>
  typeof value === 'string' && ['all', 'development', 'release'].includes(value);
export const validUpdatePage = (value: unknown): value is string =>
  typeof value === 'string' && /^[1-9]\d{0,5}$/.test(value);
export type ProjectStatus = {
  name: string;
  stage: 'development';
  minecraft: string;
  platforms: string[];
  repository: string;
};
type RecordValue = Record<string, unknown>;
const record = (x: unknown): x is RecordValue =>
  typeof x === 'object' && x !== null && !Array.isArray(x);
const text = (x: unknown): x is string => typeof x === 'string' && x.trim().length > 0;
export const validSlug = (x: unknown): x is string =>
  typeof x === 'string' && x.length <= 100 && /^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(x);
const date = (x: unknown): x is string =>
  typeof x === 'string' && Number.isFinite(Date.parse(x)) && new Date(x).toISOString() === x;
const positive = (x: unknown): x is number => Number.isSafeInteger(x) && Number(x) > 0;
const unique = (items: { id: string }[]) => new Set(items.map((x) => x.id)).size === items.length;

export function isFeature(x: unknown): x is Feature {
  return (
    record(x) &&
    validSlug(x.id) &&
    validSlug(x.slug) &&
    text(x.name) &&
    text(x.summary) &&
    text(x.details) &&
    ['hud', 'comfort', 'launcher', 'performance'].includes(String(x.category)) &&
    ['planned', 'prototype', 'verified', 'released'].includes(String(x.status)) &&
    (x.releaseId === undefined || validSlug(x.releaseId)) &&
    (x.status !== 'released' || validSlug(x.releaseId))
  );
}
export function isUpdate(x: unknown): x is Update {
  return (
    record(x) &&
    validSlug(x.id) &&
    validSlug(x.slug) &&
    text(x.title) &&
    text(x.summary) &&
    date(x.publishedAt) &&
    ['development', 'release'].includes(String(x.type)) &&
    Array.isArray(x.blocks) &&
    x.blocks.length > 0 &&
    x.blocks.every((b) => record(b) && text(b.heading) && text(b.text)) &&
    (x.releaseId === undefined || validSlug(x.releaseId)) &&
    (x.type !== 'release' || validSlug(x.releaseId))
  );
}
export function allowedDownload(url: unknown, fileName: unknown): boolean {
  if (!text(url) || !text(fileName)) return false;
  try {
    const parsed = new URL(url);
    const parts = parsed.pathname.split('/');
    return (
      parsed.protocol === 'https:' &&
      parsed.hostname === 'github.com' &&
      !parsed.port &&
      !parsed.username &&
      !parsed.password &&
      !parsed.search &&
      !parsed.hash &&
      parts.length === 7 &&
      parts[1] === 'AdilSatuv' &&
      parts[2] === 'spaceclient' &&
      parts[3] === 'releases' &&
      parts[4] === 'download' &&
      parts[5].length > 0 &&
      decodeURIComponent(parts[6]) === fileName
    );
  } catch {
    return false;
  }
}
function isArtifact(x: unknown): x is Artifact {
  return (
    record(x) &&
    x.platform === 'windows' &&
    x.architecture === 'x64' &&
    text(x.fileName) &&
    /^[A-Za-z0-9][A-Za-z0-9._-]*\.(exe|msi|zip)$/.test(x.fileName) &&
    positive(x.sizeBytes) &&
    typeof x.sha256 === 'string' &&
    /^[a-fA-F0-9]{64}$/.test(x.sha256) &&
    ['signed', 'unsigned', 'unknown'].includes(String(x.signatureStatus)) &&
    allowedDownload(x.downloadUrl, x.fileName)
  );
}
export function isRelease(x: unknown): x is Release {
  return (
    record(x) &&
    validSlug(x.id) &&
    typeof x.version === 'string' &&
    /^\d+\.\d+\.\d+(?:-[a-z0-9.-]+)?$/.test(x.version) &&
    ['alpha', 'beta', 'stable'].includes(String(x.channel)) &&
    date(x.publishedAt) &&
    validSlug(x.changelogSlug) &&
    Array.isArray(x.minecraftVersions) &&
    x.minecraftVersions.length > 0 &&
    x.minecraftVersions.every((v) => typeof v === 'string' && /^\d+\.\d+(?:\.\d+)?$/.test(v)) &&
    Array.isArray(x.artifacts) &&
    x.artifacts.length > 0 &&
    x.artifacts.every(isArtifact) &&
    (x.withdrawn === undefined || typeof x.withdrawn === 'boolean')
  );
}
export function isFeatureList(x: unknown): x is { items: Feature[] } {
  return (
    record(x) &&
    Array.isArray(x.items) &&
    x.items.every(isFeature) &&
    unique(x.items) &&
    new Set(x.items.map((item) => item.slug)).size === x.items.length
  );
}
export function isReleaseList(x: unknown): x is { items: Release[]; total: number } {
  return (
    record(x) &&
    Array.isArray(x.items) &&
    x.items.every(isRelease) &&
    unique(x.items) &&
    x.total === x.items.length
  );
}
export function isUpdateList(x: unknown): x is UpdateList {
  return (
    record(x) &&
    Array.isArray(x.items) &&
    x.items.every(isUpdate) &&
    unique(x.items) &&
    new Set(x.items.map((item) => item.slug)).size === x.items.length &&
    positive(x.page) &&
    x.pageSize === UPDATE_PAGE_SIZE &&
    x.items.length <= UPDATE_PAGE_SIZE &&
    Number.isSafeInteger(x.total) &&
    Number(x.total) >= x.items.length &&
    x.items.length ===
      Math.min(
        UPDATE_PAGE_SIZE,
        Math.max(0, Number(x.total) - (Number(x.page) - 1) * UPDATE_PAGE_SIZE),
      )
  );
}

export function isProjectStatus(value: unknown): value is ProjectStatus {
  return (
    record(value) &&
    text(value.name) &&
    value.stage === 'development' &&
    text(value.minecraft) &&
    Array.isArray(value.platforms) &&
    value.platforms.length > 0 &&
    value.platforms.every(text) &&
    value.repository === 'https://github.com/AdilSatuv/spaceclient'
  );
}

export function validateContent(value: unknown): asserts value is ContentCatalog {
  if (
    !record(value) ||
    !isFeatureList({ items: value.features }) ||
    !isReleaseList({
      items: value.releases,
      total: Array.isArray(value.releases) ? value.releases.length : -1,
    }) ||
    !Array.isArray(value.updates) ||
    !value.updates.every(isUpdate) ||
    !unique(value.updates) ||
    new Set(value.updates.map((item) => item.slug)).size !== value.updates.length
  ) {
    throw new Error('Invalid content catalog');
  }
  const catalog = value as ContentCatalog;
  const ids = new Set(catalog.releases.map((item) => item.id));
  for (const item of [...catalog.features, ...catalog.updates]) {
    if (item.releaseId !== undefined && !ids.has(item.releaseId))
      throw new Error('Unknown content release');
  }
  for (const release of catalog.releases) {
    if (
      !catalog.updates.some(
        (item) =>
          item.slug === release.changelogSlug &&
          item.type === 'release' &&
          item.releaseId === release.id,
      )
    ) {
      throw new Error('Missing release changelog');
    }
    if (
      new Set(release.artifacts.map((item) => `${item.platform}/${item.architecture}`)).size !==
      release.artifacts.length
    ) {
      throw new Error('Duplicate release artifact');
    }
  }
}
