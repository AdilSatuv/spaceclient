import fs from 'node:fs/promises';
import path from 'node:path';
import { spawn, execFileSync } from 'node:child_process';
import { createHash } from 'node:crypto';
import { crc32, deflateSync } from 'node:zlib';
import { buildVersionPlan } from '../../../apps/launcher/src/main/version-plan.ts';
const chunk = (type, data) => {
  const bytes = Buffer.alloc(data.length + 12);
  bytes.writeUInt32BE(data.length);
  bytes.write(type, 4);
  data.copy(bytes, 8);
  bytes.writeUInt32BE(crc32(bytes.subarray(4, -4)), bytes.length - 4);
  return bytes;
};
function png(w, h, { raw }) {
  const header = Buffer.alloc(13);
  header.writeUInt32BE(w);
  header.writeUInt32BE(h, 4);
  header[8] = 8;
  header[9] = 6;
  return Buffer.concat([
    Buffer.from('89504e470d0a1a0a', 'hex'),
    chunk('IHDR', header),
    chunk('IDAT', deflateSync(raw)),
    chunk('IEND', Buffer.alloc(0)),
  ]);
}
const repo = process.cwd(),
  qa = path.join(repo, '.reports/cosmetics-qa'),
  root = path.join(repo, '.reports/cosmetics-runtime');
const old = path.join(repo, '.reports/menu-help-runtime');
await fs.mkdir(root, { recursive: true });
async function linkTree(from, to) {
  await fs.mkdir(to, { recursive: true });
  for (const e of await fs.readdir(from, { withFileTypes: true })) {
    const a = path.join(from, e.name),
      b = path.join(to, e.name);
    if (e.isDirectory()) await linkTree(a, b);
    else if (e.isFile())
      await fs.link(a, b).catch((e) => {
        if (e.code !== 'EEXIST') throw e;
      });
  }
}
for (const dir of ['libraries', 'assets', 'versions', 'natives'])
  await linkTree(path.join(old, dir), path.join(root, dir));
for (const dir of ['mods', 'config/space-appearance'])
  await fs.mkdir(path.join(root, dir), { recursive: true });
const model = process.argv[2] || 'slim';
if (!['classic', 'slim'].includes(model)) throw Error('Invalid fixture model');
function image(w, h, color) {
  const raw = Buffer.alloc((w * 4 + 1) * h);
  for (let y = 0; y < h; y++)
    for (let x = 0; x < w; x++) Buffer.from(color(x, y)).copy(raw, y * (w * 4 + 1) + 1 + x * 4);
  return png(w, h, { raw });
}
const skin = image(64, model === 'classic' ? 32 : 64, (x, y) => [
    x < 32 ? 235 : 100,
    y < 32 ? 110 : 210,
    195,
    255,
  ]),
  cape = image(128, 64, () => [180, 70, 110, 255]);
const hash = (b) => createHash('sha256').update(b).digest('hex'),
  skinHash = hash(skin),
  capeHash = hash(cape);
await fs.writeFile(path.join(root, 'config/space-appearance/skin-' + skinHash + '.png'), skin);
await fs.writeFile(path.join(root, 'config/space-appearance/cape-' + capeHash + '.png'), cape);
await fs.writeFile(
  path.join(root, 'config/space-appearance.properties'),
  `schemaVersion=1\naccountId=00000000-0000-4000-8000-000000000001\nusername=CosmeticsQA\nnickname=CosmeticsQA\nskinSha256=${skinHash}\nskinModel=${model}\ncapeSha256=${capeHash}\n`,
);
await fs.writeFile(
  path.join(root, 'config/spaceclient-cosmetics.properties'),
  'schemaVersion=1\nenabled=true\nwings=AURORA\nhalo=ORBIT\n',
);
await fs.writeFile(
  path.join(root, 'options.txt'),
  'guiScale:2\npauseOnLostFocus:false\nmaxFps:60\nrenderDistance:4\nsoundCategory_master:0.0\nlang:en_US\n',
);
for (const f of ['qa-cosmetics-result.json', 'qa-cosmetics-failure.txt'])
  await fs.unlink(path.join(root, f)).catch((e) => {
    if (e.code !== 'ENOENT') throw e;
  });
const jar = path.join(repo, 'game/build/libs/space-client-0.1.0.jar'),
  bytes = await fs.readFile(jar),
  sha = hash(bytes),
  tree = execFileSync('git', ['rev-parse', 'HEAD:game'], { cwd: repo, encoding: 'utf8' }).trim();
await fs.copyFile(jar, path.join(root, 'mods/space-client.jar'));
const plan = buildVersionPlan(
  JSON.parse(await fs.readFile(path.join(root, 'versions/1.8.9/1.8.9.json'), 'utf8')),
  JSON.parse(await fs.readFile(path.join(root, 'assets/indexes/1.8.json'), 'utf8')),
);
let natives;
for (const e of await fs.readdir(path.join(root, 'natives'), { withFileTypes: true }))
  if (
    e.isDirectory() &&
    (await fs.readdir(path.join(root, 'natives', e.name))).includes('lwjgl-windows-amd64.dll')
  ) {
    natives = path.join(root, 'natives', e.name);
    break;
  }
if (!natives) throw Error('No native cache');
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
  'CosmeticsQA',
  '--version',
  'Space-1.8.9',
  '--gameDir',
  root,
  '--assetsDir',
  path.join(root, 'assets'),
  '--assetIndex',
  '1.8',
  '--uuid',
  '00000000000040008000000000000001',
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
const log = await fs.open(path.join(qa, 'native-' + model + '.log'), 'w'),
  child = spawn(path.join(repo, '.tools/jdk-21/bin/java.exe'), args, {
    cwd: root,
    windowsHide: true,
    stdio: ['ignore', log.fd, log.fd],
  });
await fs.writeFile(
  path.join(qa, 'run-' + model + '.json'),
  JSON.stringify({ pid: child.pid, tree, sha256: sha, root, model }),
);
const timer = setTimeout(() => child.kill(), 150000);
const code = await new Promise((resolve, reject) => {
  child.once('error', reject);
  child.once('exit', resolve);
});
clearTimeout(timer);
await log.close();
const failure = await fs
  .readFile(path.join(root, 'qa-cosmetics-failure.txt'), 'utf8')
  .catch(() => null);
if (failure) throw Error(failure);
const result = JSON.parse(await fs.readFile(path.join(root, 'qa-cosmetics-result.json'), 'utf8')),
  loaded = await fs.readFile(path.join(root, 'qa-loaded-build.txt'), 'utf8');
if (code !== 0 || !result.ok || loaded !== tree + '\ndev-' + tree.substring(0, 12))
  throw Error('QA exit or loaded source stamp mismatch');
await fs.cp(path.join(root, 'qa-captures'), path.join(qa, 'captures-' + model), {
  recursive: true,
});
await fs.writeFile(
  path.join(qa, 'result-' + model + '.json'),
  JSON.stringify({ ...result, exitCode: code, gameSourceTree: tree, sha256: sha, model }),
);
console.log(
  JSON.stringify({ ...result, exitCode: code, gameSourceTree: tree, sha256: sha, model }),
);
