import { UserFacingError } from './application/user-facing-error.ts';
import { createHash, randomUUID } from 'node:crypto';
import { lstat, readFile, writeFile, rename, unlink } from 'node:fs/promises';
import { join, dirname } from 'node:path';
import { crc32 } from 'node:zlib';
import { fromBufferPromise } from 'yauzl';
import { installationTarget } from './downloads.ts';
import type { SpaceBuildIdentity } from '../shared/build-identity.ts';

export type SpaceBundleManifest = SpaceBuildIdentity & {
  schemaVersion: 1;
};
export type InstalledSpace = SpaceBundleManifest & { relativePath: 'mods/space-client.jar' };
const expectedMixins = [
  'ClientMixin',
  'QueuedPlayPacketMixin',
  'HudMixin',
  'GameMenuMixin',
  'AutoSprintMixin',
  'FreeLookRendererMixin',
  'FreeLookInputMixin',
  'MenuButtonMixin',
  'MenuScreenMixin',
  'TitleMenuMixin',
  'ZoomRendererMixin',
  'CrosshairMixin',
  'FullbrightMixin',
  'TimeChangerMixin',
  'NoHurtCamMixin',
  'WeatherWorldMixin',
  'WeatherRendererMixin',
  'ChatTimestampMixin',
  'BlockOverlayMixin',
  'HitColorMixin',
  'FovControlMixin',
  'HeldPresentationMixin',
  'DroppedItemMixin',
  'HitboxesMixin',
  'GuiScaleMixin',
  'SoundSystemMixin',
  'SoundAttackMixin',
  'SoundDeathMixin',
  'ScoreboardMixin',
  'TooltipMixin',
  'ParticlesMixin',
  'GlintItemMixin',
  'GlintArmorMixin',
  'DynamicFpsMixin',
  'TntCountdownMixin',
  'WaypointsMixin',
  'SpaceAppearanceMixin',
  'SpaceCosmeticsMixin',
  'EarlyLoadingMixin',
  'LoadingRendererMixin',
  'ProgressScreenMixin',
  'TerrainScreenMixin',
];
const required = [
  'fabric.mod.json',
  'spaceclient.mixins.json',
  'spaceclient-build.properties',
  'dev/spaceclient/SpaceClient.class',
  'dev/spaceclient/BuildIdentity.class',
  'dev/spaceclient/ClickCounter.class',
  'dev/spaceclient/HudConfig.class',
  'dev/spaceclient/mixin/ClientMixin.class',
  'dev/spaceclient/mixin/QueuedPlayPacketMixin.class',
  'dev/spaceclient/mixin/HudMixin.class',
  'dev/spaceclient/mixin/GameMenuMixin.class',
  'dev/spaceclient/mixin/AutoSprintMixin.class',
  'dev/spaceclient/mixin/FreeLookRendererMixin.class',
  'dev/spaceclient/mixin/FreeLookInputMixin.class',
  'dev/spaceclient/mixin/MenuButtonMixin.class',
  'dev/spaceclient/mixin/MenuScreenMixin.class',
  'dev/spaceclient/mixin/TitleMenuMixin.class',
  'dev/spaceclient/mixin/ZoomRendererMixin.class',
  'dev/spaceclient/mixin/CrosshairMixin.class',
  'dev/spaceclient/mixin/FullbrightMixin.class',
  'dev/spaceclient/mixin/TimeChangerMixin.class',
  'dev/spaceclient/mixin/NoHurtCamMixin.class',
  'dev/spaceclient/mixin/WeatherWorldMixin.class',
  'dev/spaceclient/mixin/WeatherRendererMixin.class',
  'dev/spaceclient/mixin/ChatTimestampMixin.class',
  'dev/spaceclient/mixin/BlockOverlayMixin.class',
  'dev/spaceclient/mixin/HitColorMixin.class',
  'dev/spaceclient/mixin/FovControlMixin.class',
  'dev/spaceclient/mixin/HeldPresentationMixin.class',
  'dev/spaceclient/mixin/DroppedItemMixin.class',
  'dev/spaceclient/mixin/HitboxesMixin.class',
  'dev/spaceclient/mixin/GuiScaleMixin.class',
  'dev/spaceclient/mixin/SoundSystemMixin.class',
  'dev/spaceclient/mixin/SoundAttackMixin.class',
  'dev/spaceclient/mixin/SoundDeathMixin.class',
  'dev/spaceclient/mixin/ScoreboardMixin.class',
  'dev/spaceclient/mixin/TooltipMixin.class',
  'dev/spaceclient/mixin/ParticlesMixin.class',
  'dev/spaceclient/mixin/GlintItemMixin.class',
  'dev/spaceclient/mixin/GlintArmorMixin.class',
  'dev/spaceclient/mixin/DynamicFpsMixin.class',
  'dev/spaceclient/mixin/TntCountdownMixin.class',
  'dev/spaceclient/mixin/WaypointsMixin.class',
  'dev/spaceclient/mixin/SpaceAppearanceMixin.class',
  'dev/spaceclient/mixin/SpaceCosmeticsMixin.class',
  'dev/spaceclient/mixin/EarlyLoadingMixin.class',
  'dev/spaceclient/mixin/LoadingRendererMixin.class',
  'dev/spaceclient/mixin/ProgressScreenMixin.class',
  'dev/spaceclient/mixin/TerrainScreenMixin.class',
  'dev/spaceclient/cosmetics/AppearanceProfile.class',
  'dev/spaceclient/cosmetics/CosmeticsSettings.class',
  'dev/spaceclient/cosmetics/CosmeticsSettings$State.class',
  'dev/spaceclient/cosmetics/CosmeticsSettings$Wings.class',
  'dev/spaceclient/cosmetics/CosmeticsSettings$Halo.class',
  'dev/spaceclient/cosmetics/CosmeticsSettings$Tint.class',
  'dev/spaceclient/cosmetics/CosmeticsRuntime.class',
  'dev/spaceclient/cosmetics/CosmeticsRuntime$Data.class',
  'dev/spaceclient/cosmetics/CosmeticsScreen.class',
  'dev/spaceclient/cosmetics/CosmeticFeature.class',
  'dev/spaceclient/hud/HudArrangeScreen.class',
  'dev/spaceclient/hud/HudLayoutHistory.class',
  'dev/spaceclient/hud/HudLayoutOperations.class',
  'dev/spaceclient/hud/HudLayoutOperations$Align.class',
  'dev/spaceclient/hud/ModuleBrowserSettings$View.class',
  'dev/spaceclient/menu/LoadingTheme.class',
  'dev/spaceclient/menu/UiTranslations.class',
  'assets/spaceclient/lang/ui_ru.tsv',

  'dev/spaceclient/mods/ModSettings.class',
  'dev/spaceclient/mods/ModSettings$Brightness.class',
  'dev/spaceclient/mods/ModSettings$TimePreset.class',
  'dev/spaceclient/mods/ModSettings$SnapPerspective.class',
  'dev/spaceclient/mods/ModSettings$WeatherMode.class',
  'dev/spaceclient/mods/SnaplookState.class',
  'dev/spaceclient/mods/SnaplookRuntime.class',
  'dev/spaceclient/mods/WeatherRuntime.class',
  'dev/spaceclient/mods/ModSettings$ChatTimeFormat.class',
  'dev/spaceclient/mods/ModSettings$DurabilityThreshold.class',
  'dev/spaceclient/mods/ChatTimestamp.class',
  'dev/spaceclient/mods/ChatTimestampRuntime.class',
  'dev/spaceclient/mods/VisualPalette.class',
  'dev/spaceclient/mods/ModSettings$BlockMode.class',
  'dev/spaceclient/mods/BlockOverlayRenderer.class',
  'dev/spaceclient/mods/HitColorRuntime.class',
  'dev/spaceclient/mods/FovRuntime.class',
  'dev/spaceclient/mods/VisualModPreview.class',
  'dev/spaceclient/mods/AutoTextGate.class',
  'dev/spaceclient/mods/AutoTextRuntime.class',
  'dev/spaceclient/mods/SwingClock.class',
  'dev/spaceclient/mods/AnimationsRuntime.class',
  'dev/spaceclient/mods/GuiScaleRuntime.class',
  'dev/spaceclient/mods/ItemPresentation.class',
  'dev/spaceclient/mods/HitboxRenderer.class',
  'dev/spaceclient/mods/RecentTarget.class',
  'dev/spaceclient/mods/SoundPolicy.class',
  'dev/spaceclient/mods/SoundRuntime.class',
  'dev/spaceclient/mods/CombatChain.class',
  'dev/spaceclient/mods/CombatMetricsRuntime.class',
  'dev/spaceclient/mods/NutritionRuntime.class',
  'dev/spaceclient/mods/IdleFramePolicy.class',
  'dev/spaceclient/mods/DynamicFpsRuntime.class',
  'dev/spaceclient/mods/ScoreboardRenderer.class',
  'dev/spaceclient/mods/ParticleRuntime.class',
  'dev/spaceclient/mods/GlintRuntime.class',
  'dev/spaceclient/mods/TooltipScroll.class',
  'dev/spaceclient/mods/TooltipRuntime.class',
  'dev/spaceclient/mods/WaypointStore.class',
  'dev/spaceclient/mods/WaypointStore$Point.class',
  'dev/spaceclient/mods/WaypointRuntime.class',
  'dev/spaceclient/mods/WaypointsScreen.class',
  'dev/spaceclient/mods/WorldLabel.class',
  'dev/spaceclient/mods/InventoryBlurRenderer.class',
  'dev/spaceclient/mods/GaussianKernel.class',

  'dev/spaceclient/mods/ModSettings$SoundTheme.class',
  'dev/spaceclient/mods/ModSettings$KillChime.class',
  'dev/spaceclient/mods/DurabilityAlert.class',
  'dev/spaceclient/hud/DurabilityWarningRenderer.class',
  'dev/spaceclient/mods/ModSettings$Options.class',
  'dev/spaceclient/mods/ModSettings$SprintMode.class',
  'dev/spaceclient/mods/GameplayModule.class',
  'dev/spaceclient/mods/GameModuleSettingsScreen.class',
  'dev/spaceclient/settings/ModuleSettingsScreen.class',
  'dev/spaceclient/settings/SettingRow.class',
  'dev/spaceclient/settings/SettingRow$Kind.class',
  'dev/spaceclient/settings/SettingNumber.class',
  'dev/spaceclient/settings/SettingsLayout.class',
  'dev/spaceclient/mods/WaypointEdit.class',
  'dev/spaceclient/mods/ModuleKeys.class',
  'dev/spaceclient/mods/ModuleShortcuts.class',
  'dev/spaceclient/mods/SprintState.class',
  'dev/spaceclient/mods/SprintRuntime.class',
  'dev/spaceclient/mods/ZoomState.class',
  'dev/spaceclient/mods/ZoomRuntime.class',
  'dev/spaceclient/mods/CrosshairStyle.class',
  'dev/spaceclient/mods/CrosshairStyle$Shape.class',
  'dev/spaceclient/mods/CrosshairStyle$Color.class',
  'dev/spaceclient/mods/CrosshairRenderer.class',
  'dev/spaceclient/freelook/FreeLookState.class',
  'dev/spaceclient/freelook/FreeLookRuntime.class',
  'dev/spaceclient/hud/HudPlacement.class',
  'dev/spaceclient/hud/HudProfile.class',
  'dev/spaceclient/hud/HudRenderer.class',
  'dev/spaceclient/hud/HudEditorLayout.class',
  'dev/spaceclient/hud/HudEditorScreen.class',
  'dev/spaceclient/hud/HudEditorConfirmation.class',
  'dev/spaceclient/hud/HudModule.class',
  'dev/spaceclient/hud/HudAppearance.class',
  'dev/spaceclient/hud/HudAppearance$Color.class',
  'dev/spaceclient/hud/HudAppearance$Backdrop.class',
  'dev/spaceclient/hud/HudContentSettings.class',
  'dev/spaceclient/hud/HudContentSettings$ClockFormat.class',
  'dev/spaceclient/hud/HudContentSettings$CpsMode.class',
  'dev/spaceclient/hud/HudContentSettings$KeyLayout.class',
  'dev/spaceclient/hud/HudContentSettings$SprintDisplay.class',
  'dev/spaceclient/hud/HudSettings.class',
  'dev/spaceclient/hud/HudModulesLayout.class',
  'dev/spaceclient/hud/HudModulesScreen.class',
  'dev/spaceclient/hud/ModuleBrowserSettings.class',
  'dev/spaceclient/hud/HudModuleIcons.class',
  'dev/spaceclient/hud/HudViewport.class',
  'dev/spaceclient/hud/HudSection.class',
  'dev/spaceclient/hud/HudSwitch.class',
  'dev/spaceclient/hud/HudSwitch$Motion.class',
  'dev/spaceclient/hud/UiShapes.class',
  'dev/spaceclient/hud/CatalogModule.class',
  'dev/spaceclient/hud/ModuleFilter.class',
  'dev/spaceclient/hud/SessionTimer.class',
  'dev/spaceclient/hud/SprintStatus.class',
  'dev/spaceclient/hud/ContextHud.class',
  'dev/spaceclient/hud/WorldHud.class',
  'dev/spaceclient/hud/WorldLabels.class',
  'dev/spaceclient/hud/ExperienceProgress.class',
  'dev/spaceclient/hud/PlayerStatsHud.class',
  'dev/spaceclient/hud/InventoryHud.class',
  'dev/spaceclient/hud/ContextLabels.class',
  'dev/spaceclient/hud/EquipmentContent.class',
  'dev/spaceclient/hud/EquipmentContent$Style.class',
  'dev/spaceclient/hud/EquipmentContent$Durability.class',
  'dev/spaceclient/hud/EquipmentLayout.class',
  'dev/spaceclient/hud/HudIconState.class',
  'dev/spaceclient/hud/EquipmentHudRenderer.class',
  'dev/spaceclient/hud/ArmorHud.class',
  'dev/spaceclient/hud/ArmorDurability.class',
  'dev/spaceclient/hud/PotionHud.class',
  'dev/spaceclient/hud/PotionHud$Effect.class',
  'dev/spaceclient/hud/KeystrokesHud.class',
  'dev/spaceclient/hud/KeystrokesLayout.class',
  'dev/spaceclient/hud/KeystrokesLayout$Cell.class',
  'dev/spaceclient/hud/KeystrokesHud$Key.class',
  'dev/spaceclient/hud/ItemCounterHud.class',
  'dev/spaceclient/hud/HudAlignment.class',
  'dev/spaceclient/hud/HudModuleSettingsScreen.class',
  'dev/spaceclient/hud/SpaceUiFont.class',
  'assets/spaceclient/font/DejaVuSans.ttf',
  'assets/spaceclient/font/LICENSE-DejaVu.txt',
];
function failure(): never {
  throw new UserFacingError(
    'Игровая сборка Space повреждена или несовместима. Пересоберите game и лаунчер.',
  );
}
function stamp(input: Buffer): Record<string, string> {
  const result: Record<string, string> = {};
  for (const line of input.toString('utf8').trim().split(/\r?\n/)) {
    const match = /^([a-zA-Z]+)=([a-zA-Z0-9.-]+)$/.exec(line);
    if (!match || Object.hasOwn(result, match[1])) failure();
    result[match[1]] = match[2];
  }
  if (
    Object.keys(result).length !== 6 ||
    result.schemaVersion !== '1' ||
    result.channel !== 'development' ||
    result.modVersion !== '0.1.0' ||
    result.minecraftVersion !== '1.8.9' ||
    result.loaderVersion !== '0.18.3' ||
    !/^[a-f0-9]{40}$/.test(result.gameSourceTree)
  )
    failure();
  return result;
}

/** Read bounded bytes once, validate the JAR structure and metadata without executing it. */
export async function inspectSpaceJar(path: string, expectedTree?: string, signal?: AbortSignal) {
  signal?.throwIfAborted();
  const info = await lstat(path);
  if (!info.isFile() || info.isSymbolicLink() || info.size < 1 || info.size > 4 * 1024 * 1024)
    failure();
  const bytes = await readFile(path, { signal });
  if (bytes.length !== info.size) failure();
  const zip = await fromBufferPromise(bytes, {
    lazyEntries: true,
    decodeStrings: true,
    strictFileNames: true,
    validateEntrySizes: true,
  });
  const contents = new Map<string, Buffer>();
  const names = new Set<string>();
  let total = 0;
  try {
    if (zip.entryCount > 1024) failure();
    for await (const entry of zip.eachEntry()) {
      signal?.throwIfAborted();
      const name = entry.fileName;
      if (name.startsWith('net/minecraft/')) failure();
      const parts = name.replace(/\/$/, '').split('/');
      if (
        name.length > 220 ||
        !/^[a-z0-9_$][a-z0-9_$./+\-]*$/i.test(name) ||
        parts.some((part) => !part || part === '.' || part === '..') ||
        names.has(name.toLowerCase()) ||
        entry.isEncrypted() ||
        ![0, 8].includes(entry.compressionMethod) ||
        ((entry.externalFileAttributes >>> 16) & 0xf000) === 0xa000
      )
        failure();
      names.add(name.toLowerCase());
      total += entry.uncompressedSize;
      if (entry.uncompressedSize > 1024 * 1024 || total > 16 * 1024 * 1024) failure();
      if (name.endsWith('/')) {
        if (entry.uncompressedSize !== 0) failure();
        continue;
      }
      const stream = await zip.openReadStreamPromise(entry);
      stream.on('error', () => {});
      const abort = () => stream.destroy(new UserFacingError('Проверка сборки отменена.'));
      signal?.addEventListener('abort', abort, { once: true });
      const chunks: Buffer[] = [];
      let size = 0;
      try {
        for await (const chunk of stream) {
          signal?.throwIfAborted();
          const data = Buffer.from(chunk);
          size += data.length;
          if (size > entry.uncompressedSize || size > 1024 * 1024) failure();
          chunks.push(data);
        }
      } finally {
        signal?.removeEventListener('abort', abort);
        stream.destroy();
      }
      const data = Buffer.concat(chunks);
      if (size !== entry.uncompressedSize || crc32(data) !== entry.crc32) failure();
      if (name.endsWith('.class')) {
        if (
          data.length < 8 ||
          data.readUInt32BE(0) !== 0xcafebabe ||
          data.readUInt16BE(6) > 52 ||
          data.readUInt16BE(6) < 45 ||
          data.includes(Buffer.from('net/minecraft/client/MinecraftClient'))
        )
          failure();
      }
      if (required.includes(name)) contents.set(name, data);
    }
  } finally {
    zip.close();
  }
  if (required.some((name) => !contents.has(name))) failure();
  const properties = stamp(contents.get('spaceclient-build.properties')!);
  if (expectedTree && properties.gameSourceTree !== expectedTree)
    throw new UserFacingError(
      'Игровой JAR собран из других исходников. Выполните game build заново.',
    );
  let mod, mixin;
  try {
    mod = JSON.parse(contents.get('fabric.mod.json')!.toString('utf8'));
    mixin = JSON.parse(contents.get('spaceclient.mixins.json')!.toString('utf8'));
  } catch {
    failure();
  }
  if (
    !mod ||
    !mixin ||
    mod.schemaVersion !== 1 ||
    mod.id !== 'spaceclient' ||
    mod.version !== '0.1.0' ||
    mod.environment !== 'client' ||
    mod.depends?.minecraft !== '1.8.9' ||
    mod.depends?.fabricloader !== '>=0.18.3' ||
    !Array.isArray(mod.entrypoints?.client) ||
    mod.entrypoints.client.length !== 1 ||
    mod.entrypoints.client[0] !== 'dev.spaceclient.SpaceClient' ||
    mixin.required !== true ||
    mixin.package !== 'dev.spaceclient.mixin' ||
    mixin.compatibilityLevel !== 'JAVA_8' ||
    !Array.isArray(mixin.client) ||
    mixin.client.length !== expectedMixins.length ||
    !expectedMixins.every((name) => mixin.client.includes(name))
  )
    failure();
  signal?.throwIfAborted();
  const manifest: SpaceBundleManifest = {
    schemaVersion: 1,
    channel: 'development',
    modVersion: '0.1.0',
    minecraftVersion: '1.8.9',
    loaderVersion: '0.18.3',
    gameSourceTree: properties.gameSourceTree,
    size: bytes.length,
    sha256: createHash('sha256').update(bytes).digest('hex'),
  };
  return { manifest, bytes };
}

/** Main-only copy of a build-pinned resource; renderer never selects the source or target. */
export async function installSpaceBundle(
  root: string,
  source: string,
  pin: SpaceBundleManifest,
  signal?: AbortSignal,
): Promise<InstalledSpace> {
  const verified = await inspectSpaceJar(source, pin.gameSourceTree, signal);
  if (verified.manifest.sha256 !== pin.sha256 || verified.manifest.size !== pin.size) failure();
  const relativePath = 'mods/space-client.jar' as const;
  const target = await installationTarget(root, relativePath);
  let temporary: string | undefined;
  try {
    try {
      const existing = await lstat(target);
      if (!existing.isFile() || existing.isSymbolicLink())
        throw new UserFacingError('Файл Space не может быть каталогом или ссылкой.');
      if (
        existing.size === pin.size &&
        createHash('sha256')
          .update(await readFile(target, { signal }))
          .digest('hex') === pin.sha256
      )
        return { ...verified.manifest, relativePath };
    } catch (error) {
      if ((error as NodeJS.ErrnoException).code !== 'ENOENT') throw error;
    }
    signal?.throwIfAborted();
    temporary = join(dirname(target), `space-client-${randomUUID()}.tmp`);
    await writeFile(temporary, verified.bytes, { flag: 'wx', mode: 0o600, signal });
    signal?.throwIfAborted();
    await rename(temporary, target);
    temporary = undefined;
    return { ...verified.manifest, relativePath };
  } finally {
    if (temporary) await unlink(temporary).catch(() => {});
  }
}
