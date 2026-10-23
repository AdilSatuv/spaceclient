const assert = require('node:assert/strict');
const { createHash } = require('node:crypto');
const { createReadStream, existsSync } = require('node:fs');
const { mkdir, writeFile } = require('node:fs/promises');
const path = require('node:path');
const { spawn } = require('node:child_process');
const {
  Browser,
  BrowserPlatform,
  Cache,
  computeExecutablePath,
  detectBrowserPlatform,
  install,
} = require('@puppeteer/browsers');

const version = '158.0.1';
const buildId = `stable_${version}`;
const cacheDir = path.resolve(__dirname, '../.tools/browsers');
// Mozilla's SHA256SUMS for this exact release; updating the browser requires new pins.
const hashes = {
  [BrowserPlatform.WIN64]: '3259a2f8508e58cf2e383b6c99bfc64d6ddc86d1407b7c2f86f660af955b5d10',
  [BrowserPlatform.LINUX]: 'dce45470139242cf294eb6ada3d192a9bb334696842b02b71ade448a139eb41a',
};
function firefoxExecutable() {
  return computeExecutablePath({ browser: Browser.FIREFOX, buildId, cacheDir });
}
async function setup() {
  const platform = detectBrowserPlatform();
  const expectedHash = hashes[platform];
  assert.ok(expectedHash, 'Firefox QA setup supports Windows x64 and Linux x64.');
  const executablePath = firefoxExecutable();
  if (existsSync(executablePath)) {
    console.log(`Firefox ${version} already available: ${executablePath}`);
    return;
  }
  const options = { browser: Browser.FIREFOX, buildId, cacheDir, platform, expectedHash };
  console.log(`Downloading Firefox ${version} from archive.mozilla.org (${platform})`);
  const archive = await install({ ...options, unpack: false });
  const hash = createHash('sha256');
  for await (const chunk of createReadStream(archive)) hash.update(chunk);
  assert.equal(hash.digest('hex'), expectedHash, 'Firefox archive SHA-256 mismatch');
  if (platform === BrowserPlatform.WIN64) {
    const directory = new Cache(cacheDir).installationDir(Browser.FIREFOX, platform, buildId);
    await mkdir(directory, { recursive: true });
    // Extract only: no system install, shortcuts, registry or default-browser changes.
    await new Promise((resolve, reject) => {
      const child = spawn(archive, [`/ExtractDir=${directory}`], {
        windowsHide: true,
        env: { ...process.env, __COMPAT_LAYER: 'RunAsInvoker' },
        stdio: 'ignore',
      });
      child.once('error', reject);
      child.once('exit', (code) =>
        code === 0 ? resolve() : reject(new Error(`Firefox extraction exited ${code}`)),
      );
    });
  } else {
    await install(options);
  }
  assert.ok(existsSync(executablePath), 'Firefox executable was not extracted');
  await writeFile(
    path.join(cacheDir, 'firefox-install.json'),
    JSON.stringify(
      {
        version,
        platform,
        archiveSha256: expectedHash,
        checksumSource: `https://archive.mozilla.org/pub/firefox/releases/${version}/SHA256SUMS`,
        executablePath,
      },
      null,
      2,
    ) + '\n',
  );
  console.log(`Firefox ${version} ready: ${executablePath}`);
}
if (require.main === module)
  setup().catch((error) => {
    console.error(error);
    process.exitCode = 1;
  });
module.exports = { firefoxExecutable, version };
