// Synthetic states for the hidden renderer check only; never imported by production main.
import { app, protocol } from 'electron';
import { readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { ProfileStore } from '../../src/main/store.ts';
import { LauncherApplication } from '../../src/main/application/launcher.ts';
import { createLauncherWindow } from '../../src/main/electron/window.ts';
import { registerLauncherIpc } from '../../src/main/electron/ipc.ts';
import { createAssetHandler } from '../../src/main/electron/assets.ts';

const root = process.env.SPACE_IDENTITY_QA_ROOT;
const dist = process.env.SPACE_IDENTITY_QA_DIST;
if (!root || !dist) throw new Error('Own QA root and built renderer required.');
app.setName('Space Identity QA');
app.setPath('userData', join(root, 'data'));
protocol.registerSchemesAsPrivileged([
  { scheme: 'space', privileges: { standard: true, secure: true, supportFetchAPI: true } },
]);
const scenarioFile = join(root, 'scenario.json');
const scenario = () => JSON.parse(readFileSync(scenarioFile, 'utf8'));
const save = (value) => writeFileSync(scenarioFile, JSON.stringify(value));
async function run() {
  await app.whenReady();
  const profiles = await ProfileStore.open(join(root, 'data'), 8192);
  await profiles.saveNickname('SpaceTester');
  const application = new LauncherApplication({
    profiles,
    installation: {
      running: false,
      status: (id) => {
        const value = scenario();
        if (value.statusError) throw new Error('SECRET raw fixture error');
        return { ...value.installation, profileId: id };
      },
      start: (id) => {
        const value = scenario();
        value.installation = value.nextInstallation;
        save(value);
        return { ...value.installation, profileId: id };
      },
      cancel: async (id) => ({ ...scenario().installation, profileId: id }),
      shutdown: async () => {},
    },
    launch: {
      running: false,
      status: (id) => ({ ...scenario().launch, profileId: id }),
      start: (profile) => {
        const value = scenario();
        value.launch = value.nextLaunch;
        save(value);
        return { ...value.launch, profileId: profile.id };
      },
      shutdown: async () => {},
    },
    desktop: { chooseJava: async () => null, openDirectory: async () => {} },
    inspectJava: async () => ({ usable: false, major: null, bits: null, message: 'Fixture only' }),
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
