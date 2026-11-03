import fs from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { randomUUID } from 'node:crypto';
import { LaunchService } from '../../../apps/launcher/src/main/launch.ts';
import { prepareLocalLaunch } from '../../../apps/launcher/src/main/launch-plan.ts';
import { installSpaceBundle } from '../../../apps/launcher/src/main/game-bundle.ts';

const repo = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../../..');
const qa = path.join(repo, '.reports/input-network-qa');
await fs.mkdir(qa, { recursive: true });
const lockPath = path.join(qa, 'run.lock');
const lock = await fs.open(lockPath, 'wx');
await lock.writeFile(String(process.pid));
const sync = path.join(qa, 'run-' + randomUUID());
const hostRoot = path.join(repo, '.reports/native-menu-qa/instances/default/minecraft');
const clientRoot = path.join(repo, '.reports/network-client-qa/instances/default/minecraft');
const services = [];
const backups = new Map();
let statusTimer;
let completed = false;

async function exists(name) {
  return fs.access(path.join(sync, name)).then(
    () => true,
    () => false,
  );
}
async function waitFor(name, service, profile) {
  const started = Date.now();
  while (!(await exists(name))) {
    if (Date.now() - started > 150000) throw Error('Timeout: ' + name);
    const failures = (await fs.readdir(sync)).filter((x) => x.endsWith('-failure.txt'));
    if (failures.length) throw Error(await fs.readFile(path.join(sync, failures[0]), 'utf8'));
    if (service && ['error', 'exited'].includes(service.status(profile.id).phase))
      throw Error('QA exited before ' + name + ': ' + JSON.stringify(service.status(profile.id)));
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
}
async function linkArtifacts(source, target) {
  await fs.mkdir(target, { recursive: true });
  for (const entry of await fs.readdir(source, { withFileTypes: true })) {
    const from = path.join(source, entry.name),
      to = path.join(target, entry.name);
    if (entry.isDirectory()) await linkArtifacts(from, to);
    else if (entry.isFile()) {
      try {
        await fs.link(from, to);
      } catch (error) {
        if (error.code !== 'EEXIST') throw error;
      }
    } else throw Error('Unexpected link or special file in the QA cache');
  }
}
async function saveStatuses(profile) {
  await fs.writeFile(
    path.join(sync, 'statuses.json'),
    JSON.stringify(services.map(({ role, service }) => ({ role, ...service.status(profile.id) }))),
  );
}

try {
  await fs.mkdir(sync);
  await fs.writeFile(path.join(qa, 'latest-run.json'), JSON.stringify({ sync }));
  const state = JSON.parse(
    await fs.readFile(path.join(repo, '.reports/native-menu-qa/settings.json'), 'utf8'),
  );
  const pin = JSON.parse(
    await fs.readFile(path.join(repo, 'apps/launcher/dist/game/space-build.json'), 'utf8'),
  );
  const profile = { ...state.profiles[0], memoryMb: 1536 };
  for (const sub of ['libraries', 'versions', 'assets', 'natives', 'mods'])
    await linkArtifacts(path.join(hostRoot, sub), path.join(clientRoot, sub));
  for (const root of [hostRoot, clientRoot]) {
    const options = path.join(root, 'options.txt');
    backups.set(
      options,
      await fs.readFile(options).catch((error) => {
        if (error.code === 'ENOENT') return null;
        throw error;
      }),
    );
    await fs.writeFile(
      options,
      'guiScale:2\npauseOnLostFocus:false\nmaxFps:60\nrenderDistance:4\nsoundCategory_master:0.0\nlang:en_US\n',
    );
    await installSpaceBundle(
      root,
      path.join(repo, 'apps/launcher/dist/game/space-client.jar'),
      pin,
    );
  }
  function start(role, root, nickname) {
    const service = new LaunchService(async () => root, pin, {
      prepare: async (...args) => {
        const plan = await prepareLocalLaunch(...args);
        return {
          ...plan,
          args: [
            '-Dspace.qa.context=true',
            '-Dspace.qa.role=' + role,
            '-Dspace.qa.sync=' + sync,
            '-Dfabric.addMods=' + path.join(qa, 'input-network-qa.jar'),
            ...plan.args.map((x, i, args) =>
              args[i - 1] === '--width' ? '1280' : args[i - 1] === '--height' ? '720' : x,
            ),
          ],
        };
      },
    });
    services.push({ role, service });
    service.start(profile, nickname);
    return service;
  }
  statusTimer = setInterval(() => saveStatuses(profile).catch(() => {}), 1000);
  const a = start('host-A', hostRoot, 'SpaceHost');
  await waitFor('host-A.port', a, profile);
  const client = start('client', clientRoot, 'SpaceNet');
  await waitFor('stop-host-A.flag', client, profile);
  await a.settled();
  await fs.copyFile(path.join(hostRoot, 'logs/latest.log'), path.join(sync, 'host-A.log'));
  if (await exists('client-failure.txt'))
    throw Error(await fs.readFile(path.join(sync, 'client-failure.txt'), 'utf8'));
  const b = start('host-B', hostRoot, 'SpaceHost');
  await waitFor('host-B.port', b, profile);
  await waitFor('result.json', client, profile);
  await client.settled();
  await b.settled();
  await fs.copyFile(path.join(hostRoot, 'logs/latest.log'), path.join(sync, 'host-B.log'));
  await fs.copyFile(path.join(clientRoot, 'logs/latest.log'), path.join(sync, 'client.log'));
  for (const name of ['host-A.log', 'host-B.log', 'client.log'])
    if (/\/(?:ERROR|FATAL)\]/.test(await fs.readFile(path.join(sync, name), 'utf8')))
      throw Error('Native log errors: ' + name);
  const result = JSON.parse(await fs.readFile(path.join(sync, 'result.json'), 'utf8'));
  if (!result.ok) throw Error('Native QA failed');
  const closedPlayPackets = Number(
    await fs.readFile(path.join(sync, 'closed-play-packets.txt'), 'utf8'),
  );
  if (!(closedPlayPackets > 0)) throw Error('Closed play packet guard was not exercised');
  await saveStatuses(profile);
  await fs.writeFile(path.join(sync, 'bundle.json'), JSON.stringify(pin, null, 2));
  completed = true;
  console.log(JSON.stringify({ sync, result, closedPlayPackets }));
} finally {
  if (statusTimer) clearInterval(statusTimer);
  await fs.mkdir(sync, { recursive: true });
  await Promise.all(
    ['host-A', 'host-B'].map((role) =>
      fs.writeFile(path.join(sync, 'stop-' + role + '.flag'), '1'),
    ),
  );
  if (!completed) {
    await fs.writeFile(path.join(sync, 'abort.flag'), '1');
    // Only the three processes owned by these services participate in shutdown.
    await Promise.all(services.map(({ service }) => service.settled()));
  }
  for (const [options, previous] of backups) {
    if (previous === null) await fs.unlink(options);
    else await fs.writeFile(options, previous);
  }
  await lock.close();
  await fs.unlink(lockPath);
}
