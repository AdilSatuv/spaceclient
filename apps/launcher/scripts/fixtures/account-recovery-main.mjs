// Hidden integration entry. Uses production services/IPC/renderer and only its own QA data.
import { app, protocol } from 'electron';
import { readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { EventEmitter } from 'node:events';
import { ProfileStore } from '../../src/main/store.ts';
import { LauncherApplication } from '../../src/main/application/launcher.ts';
import { LaunchService } from '../../src/main/launch.ts';
import { AccountHttp } from '../../src/main/account/http.ts';
import { SpaceSessionVault } from '../../src/main/account/session.ts';
import { SpaceAccountService } from '../../src/main/account/service.ts';
import { windowsCacheEncryption } from '../../src/main/electron/windows-cache-encryption.ts';
import { ProfileRecovery } from '../../src/main/profile-recovery.ts';
import { writeDiagnosticFile } from '../../src/main/electron/desktop.ts';
import { createLauncherWindow } from '../../src/main/electron/window.ts';
import { registerLauncherIpc } from '../../src/main/electron/ipc.ts';
import { createAssetHandler } from '../../src/main/electron/assets.ts';

const root = process.env.SPACE_ACCOUNT_QA_ROOT,
  dist = process.env.SPACE_ACCOUNT_QA_DIST;
if (!root || !dist || !process.env.SPACE_ACCOUNT_API_URL)
  throw new Error('Own QA paths and explicit local API required.');
app.setName('Space Account QA');
app.setPath('userData', join(root, 'data'));
protocol.registerSchemesAsPrivileged([
  { scheme: 'space', privileges: { standard: true, secure: true, supportFetchAPI: true } },
]);
async function run() {
  await app.whenReady();
  const control = () => JSON.parse(readFileSync(join(root, 'control.json'), 'utf8'));
  const profiles = await ProfileStore.open(join(root, 'data'), 8192);
  if (!profiles.snapshot().localNickname) await profiles.saveNickname('GuestPlayer');
  const instance = async (id) => join(await profiles.instanceDirectory(id), 'minecraft');
  const http = new AccountHttp(process.env.SPACE_ACCOUNT_API_URL);
  if (!http.base.startsWith('http://127.0.0.1:'))
    throw new Error('QA must use explicit loopback only.');
  const account = new SpaceAccountService(
    http,
    new SpaceSessionVault(profiles.root, http.base, windowsCacheEncryption),
    {
      rootForProfile: instance,
      localNickname: () => profiles.snapshot().localNickname,
      saveNickname: (name) => profiles.saveNickname(name),
      choosePng: async (kind) => (control().cancelPicker ? null : join(root, kind + '.png')),
    },
  );
  const pin = JSON.parse(readFileSync(join(dist, 'game/space-build.json'), 'utf8'));
  const owned = new LaunchService(instance, pin, {
    beforeLaunch: (id, root, name, signal) => account.beforeLaunch(id, root, name, signal),
    prepare: async (root) => ({
      cwd: root,
      executable: 'QA never spawns Java',
      args: [],
      env: {},
      verifiedBuild: pin,
    }),
    spawn: () => {
      const props = readFileSync(
        join(profiles.root, 'instances/default/minecraft/config/space-appearance.properties'),
        'utf8',
      );
      writeFileSync(
        join(root, 'spawn-observation.json'),
        JSON.stringify({
          publicAppearanceBeforeSpawn: props.startsWith('schemaVersion=1\n'),
          containsSecret: /sessionToken|password|https?:/.test(props),
        }),
      );
      const child = new EventEmitter();
      queueMicrotask(() => {
        child.emit('spawn');
        setTimeout(() => child.emit('close', 0, null), 50);
      });
      return child;
    },
  });
  const launch = {
    get running() {
      return control().gameRunning || owned.running;
    },
    status: (id) =>
      control().gameRunning
        ? {
            profileId: id,
            mode: 'local',
            phase: 'running',
            message: 'Synthetic running-game guard',
            exitCode: null,
            log: [],
            verifiedBuild: null,
          }
        : owned.status(id),
    start: (...args) => owned.start(...args),
    shutdown: () => owned.shutdown(),
  };
  const installation = {
    running: false,
    status: (id) => ({
      profileId: id,
      phase: 'idle',
      completedFiles: 0,
      totalFiles: 0,
      receivedBytes: 0,
      verifiedBytes: 0,
      totalBytes: 0,
      retryCount: 0,
      spaceVersion: null,
      bundledBuild: pin,
      message: 'QA does not download game files.',
    }),
    start: () => {
      throw new Error('Game download forbidden in this QA.');
    },
    cancel: async () => {},
    shutdown: async () => {},
  };
  const recovery = new ProfileRecovery(profiles.root, {
    profiles,
    installation: (id) => installation.status(id),
    launch: (id) => launch.status(id),
    build: pin,
    exportReport: async (bytes) => {
      if (control().cancelPicker) return false;
      await writeDiagnosticFile(join(root, 'diagnostics.json'), bytes);
      return true;
    },
  });
  const application = new LauncherApplication({
    profiles,
    account,
    recovery,
    launch,
    installation,
    desktop: { chooseJava: async () => null, openDirectory: async () => {} },
    inspectJava: async () => ({
      usable: false,
      major: null,
      bits: null,
      message: 'No Java probe in account QA.',
    }),
  });
  protocol.handle('space', createAssetHandler(join(dist, 'renderer')));
  const window = createLauncherWindow(join(dist, 'preload.cjs'), false);
  registerLauncherIpc(application.commands, () => window);
  app.on('window-all-closed', () => app.quit());
  await window.loadURL('space://app/index.html');
}
void run().catch((error) => {
  writeFileSync(join(root, 'main-error.txt'), String(error.stack));
  app.exit(1);
});
