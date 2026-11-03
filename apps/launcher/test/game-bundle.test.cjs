const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const { makeZip } = require('./zip-fixture.cjs');
const tree = 'a'.repeat(40);
const header = Buffer.from([0xca, 0xfe, 0xba, 0xbe, 0, 0, 0, 52]);
function entries() {
  const mod = {
    schemaVersion: 1,
    id: 'spaceclient',
    version: '0.1.0',
    environment: 'client',
    depends: { minecraft: '1.8.9', fabricloader: '>=0.18.3' },
    entrypoints: { client: ['dev.spaceclient.SpaceClient'] },
  };
  const mixin = {
    required: true,
    package: 'dev.spaceclient.mixin',
    compatibilityLevel: 'JAVA_8',
    client: [
      'ClientMixin',
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
    ],
  };
  return [
    { name: 'fabric.mod.json', data: JSON.stringify(mod) },
    { name: 'spaceclient.mixins.json', data: JSON.stringify(mixin) },
    {
      name: 'spaceclient-build.properties',
      data: `schemaVersion=1\ngameSourceTree=${tree}\nmodVersion=0.1.0\nminecraftVersion=1.8.9\nloaderVersion=0.18.3\nchannel=development\n`,
    },
    ...[
      'SpaceClient',
      'ClickCounter',
      'HudConfig',
      'mixin/ClientMixin',
      'mixin/HudMixin',
      'mixin/GameMenuMixin',
      'mixin/AutoSprintMixin',
      'mixin/FreeLookRendererMixin',
      'mixin/FreeLookInputMixin',
      'mixin/MenuButtonMixin',
      'mixin/MenuScreenMixin',
      'mixin/TitleMenuMixin',
      'mixin/ZoomRendererMixin',
      'mixin/CrosshairMixin',
      'mixin/FullbrightMixin',
      'mixin/TimeChangerMixin',
      'mixin/NoHurtCamMixin',
      'mixin/WeatherWorldMixin',
      'mixin/WeatherRendererMixin',
      'mixin/ChatTimestampMixin',
      'mixin/BlockOverlayMixin',
      'mixin/HitColorMixin',
      'mixin/FovControlMixin',
      'mixin/HeldPresentationMixin',
      'mixin/DroppedItemMixin',
      'mixin/HitboxesMixin',
      'mixin/GuiScaleMixin',
      'mixin/SoundSystemMixin',
      'mixin/SoundAttackMixin',
      'mixin/SoundDeathMixin',
      'mixin/ScoreboardMixin',
      'mixin/TooltipMixin',
      'mixin/ParticlesMixin',
      'mixin/GlintItemMixin',
      'mixin/GlintArmorMixin',
      'mixin/DynamicFpsMixin',
      'mixin/TntCountdownMixin',
      'mixin/WaypointsMixin',

      'mods/ModSettings',
      'mods/ModSettings$Brightness',
      'mods/ModSettings$TimePreset',
      'mods/ModSettings$SnapPerspective',
      'mods/ModSettings$WeatherMode',
      'mods/SnaplookState',
      'mods/SnaplookRuntime',
      'mods/WeatherRuntime',
      'mods/ModSettings$ChatTimeFormat',
      'mods/ModSettings$DurabilityThreshold',
      'mods/ChatTimestamp',
      'mods/ChatTimestampRuntime',
      'mods/VisualPalette',
      'mods/ModSettings$BlockMode',
      'mods/BlockOverlayRenderer',
      'mods/HitColorRuntime',
      'mods/FovRuntime',
      'mods/VisualModPreview',
      'mods/AutoTextGate',
      'mods/AutoTextRuntime',
      'mods/SwingClock',
      'mods/AnimationsRuntime',
      'mods/GuiScaleRuntime',
      'mods/ItemPresentation',
      'mods/HitboxRenderer',
      'mods/RecentTarget',
      'mods/SoundPolicy',
      'mods/SoundRuntime',
      'mods/CombatChain',
      'mods/CombatMetricsRuntime',
      'mods/NutritionRuntime',
      'mods/IdleFramePolicy',
      'mods/DynamicFpsRuntime',
      'mods/ScoreboardRenderer',
      'mods/ParticleRuntime',
      'mods/GlintRuntime',
      'mods/TooltipScroll',
      'mods/TooltipRuntime',
      'mods/WaypointStore',
      'mods/WaypointStore$Point',
      'mods/WaypointRuntime',
      'mods/WaypointsScreen',
      'mods/WorldLabel',
      'mods/InventoryBlurRenderer',
      'mods/GaussianKernel',

      'mods/ModSettings$SoundTheme',
      'mods/ModSettings$KillChime',
      'mods/DurabilityAlert',
      'hud/DurabilityWarningRenderer',
      'mods/ModSettings$Options',
      'mods/ModSettings$SprintMode',
      'mods/GameplayModule',
      'mods/GameModuleSettingsScreen',
      'mods/ModuleKeys',
      'mods/ModuleShortcuts',
      'mods/SprintState',
      'mods/SprintRuntime',
      'mods/ZoomState',
      'mods/ZoomRuntime',
      'mods/CrosshairStyle',
      'mods/CrosshairStyle$Shape',
      'mods/CrosshairStyle$Color',
      'mods/CrosshairRenderer',
      'freelook/FreeLookState',
      'freelook/FreeLookRuntime',
      'hud/HudPlacement',
      'hud/HudProfile',
      'hud/HudRenderer',
      'hud/HudEditorLayout',
      'hud/HudEditorScreen',
      'hud/HudEditorConfirmation',
      'hud/HudModule',
      'hud/HudAppearance',
      'hud/HudAppearance$Color',
      'hud/HudAppearance$Backdrop',
      'hud/HudContentSettings',
      'hud/HudContentSettings$ClockFormat',
      'hud/HudContentSettings$CpsMode',
      'hud/HudContentSettings$KeyLayout',
      'hud/HudContentSettings$SprintDisplay',
      'hud/HudSettings',
      'hud/HudModulesLayout',
      'hud/HudModulesScreen',
      'hud/HudModuleIcons',
      'hud/HudViewport',
      'hud/HudSection',
      'hud/HudSwitch',
      'hud/HudSwitch$Motion',
      'hud/UiShapes',
      'hud/CatalogModule',
      'hud/ModuleFilter',
      'hud/ModuleBrowserSettings',
      'hud/SessionTimer',
      'hud/SprintStatus',
      'hud/ContextHud',
      'hud/WorldHud',
      'hud/WorldLabels',
      'hud/ExperienceProgress',
      'hud/PlayerStatsHud',
      'hud/InventoryHud',
      'hud/ContextLabels',
      'hud/EquipmentContent',
      'hud/EquipmentContent$Style',
      'hud/EquipmentContent$Durability',
      'hud/EquipmentLayout',
      'hud/HudIconState',
      'hud/EquipmentHudRenderer',
      'hud/ArmorHud',
      'hud/ArmorDurability',
      'hud/PotionHud',
      'hud/PotionHud$Effect',
      'hud/KeystrokesHud',
      'hud/KeystrokesLayout',
      'hud/KeystrokesLayout$Cell',
      'hud/KeystrokesHud$Key',
      'hud/ItemCounterHud',
      'hud/HudAlignment',
      'hud/HudModuleSettingsScreen',
      'hud/SpaceUiFont',
    ].map((name) => ({ name: 'dev/spaceclient/' + name + '.class', data: header })),
    { name: 'assets/spaceclient/font/DejaVuSans.ttf', data: 'font fixture' },
    { name: 'assets/spaceclient/font/LICENSE-DejaVu.txt', data: 'license fixture' },
  ];
}
async function fixture(t, change = (value) => value) {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'space-bundle-'));
  assert.equal(path.dirname(root), os.tmpdir());
  t.after(() => fs.rm(root, { recursive: true, force: true }));
  const source = path.join(root, 'source.jar');
  await fs.writeFile(source, makeZip(change(entries())));
  return { root, source, ...(await import('../src/main/game-bundle.ts')) };
}
test('bundled game metadata is bound to the committed source tree and returned with a digest', async (t) => {
  const { source, inspectSpaceJar } = await fixture(t);
  const { manifest, bytes } = await inspectSpaceJar(source, tree);
  assert.equal(manifest.gameSourceTree, tree);
  assert.equal(manifest.channel, 'development');
  assert.equal(manifest.size, bytes.length);
  assert.match(manifest.sha256, /^[a-f0-9]{64}$/);
  await assert.rejects(inspectSpaceJar(source, 'b'.repeat(40)), /других исходников/);
});

test('gameplay modules and their settings cannot disappear from an otherwise valid bundle', async (t) => {
  for (const name of [
    'mods/ModSettings',
    'mods/ModSettings$Brightness',
    'mods/ModSettings$TimePreset',
    'mods/ModSettings$SnapPerspective',
    'mods/ModSettings$WeatherMode',
    'mods/SnaplookState',
    'mods/SnaplookRuntime',
    'mods/WeatherRuntime',
    'mods/ModSettings$ChatTimeFormat',
    'mods/ModSettings$DurabilityThreshold',
    'mods/ChatTimestamp',
    'mods/ChatTimestampRuntime',
    'mods/VisualPalette',
    'mods/ModSettings$BlockMode',
    'mods/BlockOverlayRenderer',
    'mods/HitColorRuntime',
    'mods/FovRuntime',
    'mods/VisualModPreview',
    'mods/AutoTextGate',
    'mods/AutoTextRuntime',
    'mods/SwingClock',
    'mods/AnimationsRuntime',
    'mods/GuiScaleRuntime',
    'mods/ItemPresentation',
    'mods/HitboxRenderer',
    'mods/RecentTarget',
    'mods/SoundPolicy',
    'mods/SoundRuntime',
    'mods/CombatChain',
    'mods/CombatMetricsRuntime',
    'mods/NutritionRuntime',
    'mods/IdleFramePolicy',
    'mods/DynamicFpsRuntime',
    'mods/ScoreboardRenderer',
    'mods/ParticleRuntime',
    'mods/GlintRuntime',
    'mods/TooltipScroll',
    'mods/TooltipRuntime',
    'mods/WaypointStore',
    'mods/WaypointStore$Point',
    'mods/WaypointRuntime',
    'mods/WaypointsScreen',
    'mods/WorldLabel',
    'mods/InventoryBlurRenderer',
    'mods/GaussianKernel',

    'mods/ModSettings$SoundTheme',
    'mods/ModSettings$KillChime',
    'mods/DurabilityAlert',
    'hud/DurabilityWarningRenderer',
    'mods/GameplayModule',
    'mods/ModuleShortcuts',
    'mods/GameModuleSettingsScreen',
    'mods/ZoomRuntime',
    'mods/CrosshairRenderer',
    'freelook/FreeLookRuntime',
    'hud/CatalogModule',
    'hud/ModuleFilter',
    'hud/ModuleBrowserSettings',
    'hud/SessionTimer',
    'hud/SprintStatus',
    'hud/ContextHud',
    'hud/ContextLabels',
    'hud/ItemCounterHud',
  ]) {
    const { source, inspectSpaceJar } = await fixture(t, (value) =>
      value.filter((entry) => entry.name !== `dev/spaceclient/${name}.class`),
    );
    await assert.rejects(inspectSpaceJar(source, tree));
  }
});
test('wrong mod, entrypoint, version or provenance cannot become a bundle', async (t) => {
  for (const mutate of [
    (mod) => {
      mod.id = 'other';
    },
    (mod) => {
      mod.version = '9.0.0';
    },
    (mod) => {
      mod.depends.minecraft = '1.20';
    },
    (mod) => {
      mod.environment = '*';
    },
    (mod) => {
      mod.entrypoints.client = ['arbitrary.Entry'];
    },
  ]) {
    const { source, inspectSpaceJar } = await fixture(t, (value) => {
      const mod = JSON.parse(value[0].data);
      mutate(mod);
      value[0].data = JSON.stringify(mod);
      return value;
    });
    await assert.rejects(inspectSpaceJar(source, tree));
  }
  for (const change of [
    (value) => value.filter((entry) => entry.name !== 'spaceclient-build.properties'),
    (value) => {
      value[2].data += 'modVersion=0.1.0\n';
      return value;
    },
    (value) => {
      value[2].data = value[2].data.replace('development', 'stable');
      return value;
    },
  ]) {
    const { source, inspectSpaceJar } = await fixture(t, change);
    await assert.rejects(inspectSpaceJar(source));
  }
});
test('named developer bytecode and newer bytecode are rejected before bundling', async (t) => {
  for (const data of [
    Buffer.concat([header, Buffer.from('net/minecraft/client/MinecraftClient')]),
    Buffer.from([0xca, 0xfe, 0xba, 0xbe, 0, 0, 0, 65]),
  ]) {
    const { source, inspectSpaceJar } = await fixture(t, (value) => {
      value[3].data = data;
      return value;
    });
    await assert.rejects(inspectSpaceJar(source));
  }
});

test('all pinned mixin classes and the exact mixin set are required', async (t) => {
  const pinned = [
    'ClientMixin',
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
  ];
  for (const name of pinned) {
    const candidate = await fixture(t, (value) =>
      value.filter((entry) => entry.name !== `dev/spaceclient/mixin/${name}.class`),
    );
    await assert.rejects(candidate.inspectSpaceJar(candidate.source));
  }
  for (const client of [
    pinned.filter((name) => name !== 'FreeLookInputMixin'),
    [...pinned, 'OtherMixin'],
    [...pinned.slice(0, -1), 'OtherMixin'],
    [...pinned.slice(0, -1), pinned[0]],
  ]) {
    const candidate = await fixture(t, (value) => {
      const mixin = JSON.parse(value[1].data);
      mixin.client = client;
      value[1].data = JSON.stringify(mixin);
      return value;
    });
    await assert.rejects(candidate.inspectSpaceJar(candidate.source));
  }
});
test('profile and editor classes cannot be omitted from the pinned game bundle', async (t) => {
  for (const name of [
    'HudPlacement',
    'HudProfile',
    'HudRenderer',
    'HudEditorLayout',
    'HudEditorScreen',
    'HudEditorConfirmation',
    'HudModule',
    'HudAppearance',
    'HudAppearance$Color',
    'HudAppearance$Backdrop',
    'HudContentSettings',
    'HudContentSettings$ClockFormat',
    'HudContentSettings$CpsMode',
    'HudContentSettings$KeyLayout',
    'HudContentSettings$SprintDisplay',
    'KeystrokesLayout',
    'KeystrokesLayout$Cell',
    'HudSettings',
    'HudModulesLayout',
    'WorldHud',
    'WorldLabels',
    'ExperienceProgress',
    'PlayerStatsHud',
    'InventoryHud',
    'EquipmentContent',
    'EquipmentContent$Style',
    'EquipmentContent$Durability',
    'EquipmentLayout',
    'HudIconState',
    'EquipmentHudRenderer',
    'ArmorHud',
    'ArmorDurability',
    'PotionHud',
    'PotionHud$Effect',
    'HudModulesScreen',
    'ModuleBrowserSettings',
    'ModuleFilter',
    'HudModuleIcons',
    'HudViewport',
    'HudSection',
    'HudSwitch',
    'HudModuleSettingsScreen',
    'SpaceUiFont',
  ]) {
    const candidate = await fixture(t, (value) =>
      value.filter((entry) => entry.name !== `dev/spaceclient/hud/${name}.class`),
    );
    await assert.rejects(candidate.inspectSpaceJar(candidate.source));
  }
});

test('font and its redistribution notice cannot be omitted from the game bundle', async (t) => {
  for (const name of [
    'assets/spaceclient/font/DejaVuSans.ttf',
    'assets/spaceclient/font/LICENSE-DejaVu.txt',
  ]) {
    const candidate = await fixture(t, (value) => value.filter((entry) => entry.name !== name));
    await assert.rejects(candidate.inspectSpaceJar(candidate.source));
  }
});

test('duplicate paths, traversal, bad CRC and oversized entries fail JAR validation', async (t) => {
  for (const change of [
    (value) => [...value, value[0]],
    (value) => [...value, { name: '../escape', data: 'x' }],
    (value) => [...value, { name: 'net/minecraft/Client.class', data: header }],
    (value) => {
      value[0].crc = 0;
      return value;
    },
    (value) => [...value, { name: 'oversized', data: 'x', method: 8, size: 2 * 1024 * 1024 }],
  ]) {
    const { source, inspectSpaceJar } = await fixture(t, change);
    await assert.rejects(inspectSpaceJar(source));
  }
});
test('a verified bundle installs at one fixed path; bad pins preserve the existing JAR', async (t) => {
  const { root, source, inspectSpaceJar, installSpaceBundle } = await fixture(t);
  const { manifest } = await inspectSpaceJar(source, tree);
  const target = path.join(root, 'instance');
  const installed = await installSpaceBundle(target, source, manifest);
  assert.equal(installed.relativePath, 'mods/space-client.jar');
  const original = await fs.readFile(path.join(target, installed.relativePath));
  assert.deepEqual(original, await fs.readFile(source));
  await installSpaceBundle(target, source, manifest);
  await fs.writeFile(path.join(target, installed.relativePath), Buffer.alloc(original.length));
  await installSpaceBundle(target, source, manifest);
  assert.deepEqual(await fs.readFile(path.join(target, installed.relativePath)), original);
  await assert.rejects(installSpaceBundle(target, source, { ...manifest, sha256: '0'.repeat(64) }));
  assert.deepEqual(await fs.readFile(path.join(target, installed.relativePath)), original);
  assert.deepEqual(await fs.readdir(path.join(target, 'mods')), ['space-client.jar']);
});
test('mod directory junctions and cancelled copies cannot redirect or replace a JAR', async (t) => {
  const { root, source, inspectSpaceJar, installSpaceBundle } = await fixture(t);
  const { manifest } = await inspectSpaceJar(source, tree);
  const target = path.join(root, 'instance');
  const outside = path.join(root, 'outside');
  await fs.mkdir(target);
  await fs.mkdir(outside);
  await fs.symlink(outside, path.join(target, 'mods'), 'junction');
  await assert.rejects(installSpaceBundle(target, source, manifest), /ссылку/);
  assert.deepEqual(await fs.readdir(outside), []);
  const controller = new AbortController();
  controller.abort();
  await assert.rejects(
    installSpaceBundle(path.join(root, 'cancelled'), source, manifest, controller.signal),
    { name: 'AbortError' },
  );
  await assert.rejects(fs.stat(path.join(root, 'cancelled')), { code: 'ENOENT' });
});
