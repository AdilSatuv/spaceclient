import { validateArtifact } from './downloads.ts';
import type { DownloadArtifact } from './downloads.ts';
import { validatePlan } from './download-queue.ts';
import { LOGGING_LIBRARIES } from './logging-libraries.ts';
import { LOADER_LIBRARIES } from './loader-libraries.ts';

export const VERSION_METADATA: Readonly<DownloadArtifact> = Object.freeze({
  url: 'https://piston-meta.mojang.com/v1/packages/d546f1707a3f2b7d034eece5ea2e311eda875787/1.8.9.json',
  sha1: 'd546f1707a3f2b7d034eece5ea2e311eda875787',
  size: 18244,
  relativePath: 'versions/1.8.9/1.8.9.json',
});
export const ASSET_INDEX: Readonly<DownloadArtifact> = Object.freeze({
  url: 'https://launchermeta.mojang.com/v1/packages/f6ad102bcaa53b1a58358f16e376d548d44933ec/1.8.json',
  sha1: 'f6ad102bcaa53b1a58358f16e376d548d44933ec',
  size: 78494,
  relativePath: 'assets/indexes/1.8.json',
});
type JsonObject = Record<string, unknown>;
export type VersionPlan = {
  version: '1.8.9';
  platform: 'windows-x64';
  loggingVersion: '2.26.1';
  loaderVersion: '0.18.3';
  mainClass: 'net.fabricmc.loader.impl.launch.knot.KnotClient';
  files: DownloadArtifact[];
  classpath: string[];
  natives: { archive: string; exclude: string[] }[];
  excludedLibraries: string[];
  assetCount: number;
  totalBytes: number;
  launchReady: false;
  blockers: string[];
};
function object(value: unknown): JsonObject {
  if (!value || typeof value !== 'object' || Array.isArray(value))
    throw new Error('Ожидался объект metadata.');
  return value as JsonObject;
}
function text(value: unknown, maximum = 220): string {
  if (typeof value !== 'string' || !value.length || value.length > maximum)
    throw new Error('Неверное строковое поле metadata.');
  return value;
}
function file(value: unknown, relativePath: string): DownloadArtifact {
  const source = object(value);
  return validateArtifact({
    url: text(source.url, 1024),
    sha1: text(source.sha1, 40),
    size: source.size as number,
    relativePath,
  });
}

/** Only the OS-name rules used by pinned 1.8.9 are supported; unknown conditions fail closed. */
export function allowedOnWindows(input: unknown): boolean {
  if (input === undefined) return true;
  if (!Array.isArray(input) || input.length > 32) throw new Error('Неверные rules библиотеки.');
  let allowed = false;
  for (const raw of input) {
    const rule = object(raw);
    if (
      Object.keys(rule).some((key) => !['action', 'os'].includes(key)) ||
      !['allow', 'disallow'].includes(rule.action as string)
    )
      throw new Error('Неподдерживаемое условие rules.');
    let matches = true;
    if (rule.os !== undefined) {
      const os = object(rule.os);
      if (
        Object.keys(os).some((key) => key !== 'name') ||
        !['windows', 'linux', 'osx'].includes(os.name as string)
      )
        throw new Error('Неподдерживаемое условие ОС.');
      matches = os.name === 'windows';
    }
    if (matches) allowed = rule.action === 'allow';
  }
  return allowed;
}

function libraryFile(raw: unknown, coordinate: string, classifier?: string): DownloadArtifact {
  const [group, name, version] = coordinate.split(':');
  const expected = `${group.replaceAll('.', '/')}/${name}/${version}/${name}-${version}${classifier ? '-' + classifier : ''}.jar`;
  const source = object(raw);
  if (source.path !== expected || source.url !== `https://libraries.minecraft.net/${expected}`)
    throw new Error('Путь библиотеки не совпадает с её координатами.');
  return file(source, 'libraries/' + expected);
}

/** Pure plan builder. Call only with metadata downloaded and hash-verified by prepareVersionPlan. */
export function buildVersionPlan(metadataInput: unknown, indexInput: unknown): VersionPlan {
  const metadata = object(metadataInput);
  const index = object(indexInput);
  if (
    metadata.id !== '1.8.9' ||
    metadata.type !== 'release' ||
    metadata.assets !== '1.8' ||
    metadata.mainClass !== 'net.minecraft.client.main.Main' ||
    metadata.inheritsFrom !== undefined
  )
    throw new Error('Поддерживаются только закреплённые metadata Minecraft 1.8.9.');
  const declaredIndex = object(metadata.assetIndex);
  const assetIndex = file(declaredIndex, ASSET_INDEX.relativePath);
  if (
    declaredIndex.id !== '1.8' ||
    assetIndex.url !== ASSET_INDEX.url ||
    assetIndex.sha1 !== ASSET_INDEX.sha1 ||
    assetIndex.size !== ASSET_INDEX.size
  )
    throw new Error('Asset index не совпадает с закреплённой версией.');
  if (index.virtual !== undefined && index.virtual !== false)
    throw new Error('Virtual assets пока не поддерживаются.');
  if (index.map_to_resources !== undefined && index.map_to_resources !== false)
    throw new Error('Копирование assets в resources пока не поддерживается.');
  const files: DownloadArtifact[] = [
    { ...VERSION_METADATA },
    { ...ASSET_INDEX },
    file(object(metadata.downloads).client, 'versions/1.8.9/1.8.9.jar'),
  ];
  const classpath: string[] = [];
  const natives: VersionPlan['natives'] = [];
  const excludedLibraries: string[] = [];
  if (!Array.isArray(metadata.libraries) || metadata.libraries.length > 256)
    throw new Error('Неверный список библиотек.');
  const coordinates = new Set<string>();
  for (const raw of metadata.libraries) {
    const library = object(raw);
    const coordinate = text(library.name);
    if (!/^[a-z0-9_-]+(?:\.[a-z0-9_-]+)*:[a-z0-9_.-]+:[a-z0-9_.-]+$/i.test(coordinate))
      throw new Error('Неверные координаты библиотеки.');
    if (coordinates.has(coordinate)) throw new Error('Повтор координат библиотеки.');
    coordinates.add(coordinate);
    if (!allowedOnWindows(library.rules)) continue;
    // Replace original logging and LWJGL families as complete controlled sets.
    if (
      coordinate.startsWith('org.apache.logging.log4j:') ||
      coordinate.startsWith('org.lwjgl.lwjgl:')
    ) {
      excludedLibraries.push(coordinate);
      continue;
    }
    const downloads = object(library.downloads);
    if (downloads.artifact !== undefined) {
      const artifact = libraryFile(downloads.artifact, coordinate);
      files.push(artifact);
      classpath.push(artifact.relativePath);
    }
    if (library.natives !== undefined) {
      const template = object(library.natives).windows;
      if (template === undefined) continue;
      const classifier = text(template, 64).replaceAll('${arch}', '64');
      if (!/^natives-windows(?:-64)?$/.test(classifier))
        throw new Error('Неподдерживаемый Windows native classifier.');
      const artifact = libraryFile(
        object(downloads.classifiers)[classifier],
        coordinate,
        classifier,
      );
      const exclusions = library.extract === undefined ? [] : object(library.extract).exclude;
      if (!Array.isArray(exclusions) || exclusions.some((item) => item !== 'META-INF/'))
        throw new Error('Неподдерживаемые правила распаковки natives.');
      files.push(artifact);
      natives.push({ archive: artifact.relativePath, exclude: [...exclusions] });
    }
  }
  for (const logging of LOGGING_LIBRARIES) {
    files.push(validateArtifact(logging));
    classpath.push(logging.relativePath);
  }
  for (const library of LOADER_LIBRARIES) {
    files.push(validateArtifact(library));
    if (library.native) natives.push({ archive: library.relativePath, exclude: ['META-INF/'] });
    else classpath.push(library.relativePath);
  }
  const objects = object(index.objects);
  const entries = Object.entries(objects);
  if (!entries.length || entries.length > 9000) throw new Error('Неверный размер asset index.');
  let logicalAssetBytes = 0;
  for (const [name, value] of entries) {
    // Names are metadata labels only. On disk assets are always addressed by hash.
    if (name.length > 256 || /[\u0000-\u001f]/.test(name)) throw new Error('Неверное имя ресурса.');
    const source = object(value);
    const hash = text(source.hash, 40);
    const artifact = file(
      {
        url: `https://resources.download.minecraft.net/${hash.slice(0, 2)}/${hash}`,
        sha1: hash,
        size: source.size,
      },
      `assets/objects/${hash.slice(0, 2)}/${hash}`,
    );
    logicalAssetBytes += artifact.size;
    files.push(artifact);
  }
  if (declaredIndex.totalSize !== logicalAssetBytes)
    throw new Error('Суммарный размер assets не совпадает с metadata.');
  const unique = validatePlan(files);
  classpath.push('versions/1.8.9/1.8.9.jar');
  return {
    version: '1.8.9',
    platform: 'windows-x64',
    loggingVersion: '2.26.1',
    loaderVersion: '0.18.3',
    mainClass: 'net.fabricmc.loader.impl.launch.knot.KnotClient',
    files: unique,
    classpath,
    natives,
    excludedLibraries,
    assetCount: entries.length,
    totalBytes: unique.reduce((sum, artifact) => sum + artifact.size, 0),
    launchReady: false,
    blockers: [
      'Не добавлен производственный Space jar.',
      'Не подтверждены Microsoft-вход, владение игрой и runtime.',
    ],
  };
}
