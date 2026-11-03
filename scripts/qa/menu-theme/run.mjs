import fs from 'node:fs/promises';
import path from 'node:path';
import { spawn, execFileSync } from 'node:child_process';
import { createHash, randomUUID } from 'node:crypto';
import { buildVersionPlan } from '../../../apps/launcher/src/main/version-plan.ts';

const repo = path.resolve(import.meta.dirname, '../../..');
const qa = path.join(repo, '.reports/menu-theme-qa');
const root = path.join(qa, 'run-' + randomUUID());
const cache = path.join(repo, '.reports/menu-help-runtime');
await fs.mkdir(root, { recursive: true });
async function linkTree(from, to) {
  await fs.mkdir(to, { recursive: true });
  for (const entry of await fs.readdir(from, { withFileTypes: true })) {
    const a = path.join(from, entry.name),
      b = path.join(to, entry.name);
    if (entry.isDirectory()) await linkTree(a, b);
    else if (entry.isFile()) await fs.link(a, b);
    else throw Error('Unexpected cache link');
  }
}
for (const name of ['libraries', 'assets', 'versions', 'natives'])
  await linkTree(path.join(cache, name), path.join(root, name));
await fs.mkdir(path.join(root, 'mods'));
await fs.writeFile(
  path.join(root, 'options.txt'),
  'guiScale:2\npauseOnLostFocus:false\nmaxFps:60\nrenderDistance:4\nsoundCategory_master:0.0\nsnooperEnabled:false\nrealmsNotifications:false\nlang:en_US\n',
);
const jar = path.join(repo, 'game/build/libs/space-client-0.1.0.jar');
const sha = createHash('sha256')
  .update(await fs.readFile(jar))
  .digest('hex');
const tree = execFileSync('git', ['rev-parse', 'HEAD:game'], {
  cwd: repo,
  encoding: 'utf8',
  windowsHide: true,
}).trim();
await fs.copyFile(jar, path.join(root, 'mods/space-client.jar'));
const plan = buildVersionPlan(
  JSON.parse(await fs.readFile(path.join(root, 'versions/1.8.9/1.8.9.json'), 'utf8')),
  JSON.parse(await fs.readFile(path.join(root, 'assets/indexes/1.8.json'), 'utf8')),
);
let natives;
for (const entry of await fs.readdir(path.join(root, 'natives'), { withFileTypes: true }))
  if (
    entry.isDirectory() &&
    (await fs.readdir(path.join(root, 'natives', entry.name))).includes('lwjgl-windows-amd64.dll')
  ) {
    natives = path.join(root, 'natives', entry.name);
    break;
  }
if (!natives) throw Error('Own native cache unavailable');
const args = [
  '-Xms256M',
  '-Xmx1024M',
  '-Dfabric.noGui=true',
  '-Dfile.encoding=UTF-8',
  '-Djava.library.path=' + natives,
  '-Dfabric.gameVersion=1.8.9',
  '-Dfabric.gameJarPath=' + path.join(root, 'versions/1.8.9/1.8.9.jar'),
  '-Dfabric.addMods=' + path.join(qa, 'settings-qa.jar'),
  '-cp',
  plan.classpath.map((p) => path.join(root, p)).join(path.delimiter),
  plan.mainClass,
  '--username',
  'SpaceMenuQA',
  '--version',
  'Space-1.8.9',
  '--gameDir',
  root,
  '--assetsDir',
  path.join(root, 'assets'),
  '--assetIndex',
  '1.8',
  '--uuid',
  '00000000000040008000000000000002',
  '--accessToken',
  '0',
  '--userProperties',
  '{}',
  '--userType',
  'legacy',
  '--width',
  '1280',
  '--height',
  '720',
];
const env = { ...process.env };
for (const key of ['JAVA_TOOL_OPTIONS', '_JAVA_OPTIONS', 'JDK_JAVA_OPTIONS', 'FABRIC_ADD_MODS'])
  delete env[key];
const log = await fs.open(path.join(root, 'native.log'), 'w');
const child = spawn(path.join(repo, '.tools/jdk-21/bin/java.exe'), args, {
  cwd: root,
  env,
  windowsHide: true,
  stdio: ['ignore', log.fd, log.fd],
});
await fs.writeFile(
  path.join(root, 'identity.json'),
  JSON.stringify({ pid: child.pid, tree, sha256: sha }),
);
const timeout = setTimeout(() => child.kill(), 180000);
let code;
try {
  code = await new Promise((resolve, reject) => {
    child.once('error', reject);
    child.once('close', resolve);
  });
} finally {
  clearTimeout(timeout);
  child.kill();
  await log.close();
}
const failure = await fs.readFile(path.join(root, 'failure.txt'), 'utf8').catch(() => null);
if (failure) throw Error(failure);
const result = JSON.parse(await fs.readFile(path.join(root, 'result.json'), 'utf8'));
const loaded = await fs.readFile(path.join(root, 'loaded-tree.txt'), 'utf8');
if (
  code !== 0 ||
  !result.ok ||
  loaded !== tree ||
  result.nativeSliderDragChecks !== 3 ||
  result.glErrors !== 0
)
  throw Error('Native menu QA failed: ' + root);
if (/\/(ERROR|FATAL)\]/.test(await fs.readFile(path.join(root, 'native.log'), 'utf8')))
  throw Error('Native log errors: ' + root);
const report = {
  ...result,
  exitCode: code,
  gameSourceTree: tree,
  sha256: sha,
  root,
  noOsInput: true,
  noLanDiscovery: true,
  privateWorld: true,
};
await fs.writeFile(path.join(root, 'report.json'), JSON.stringify(report, null, 2) + '\n');
await fs.writeFile(path.join(qa, 'latest-run.json'), JSON.stringify(report, null, 2) + '\n');
console.log(JSON.stringify(report));
