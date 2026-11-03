import fs from 'node:fs/promises';
import path from 'node:path';
import { spawn, execFileSync } from 'node:child_process';
import { randomUUID, createHash } from 'node:crypto';
import { buildVersionPlan } from '../../../apps/launcher/src/main/version-plan.ts';
const repo = path.resolve(import.meta.dirname, '../../..'),
  qa = path.join(repo, '.reports/native-perf-qa');
await fs.mkdir(qa, { recursive: true });
const lockPath = path.join(qa, 'run.lock'),
  lock = await fs.open(lockPath, 'wx');
let child;
try {
  await lock.writeFile(String(process.pid));
  const runRoot = path.join(qa, 'run-' + randomUUID()),
    cache = path.join(repo, '.reports/menu-help-runtime');
  await fs.mkdir(runRoot);
  const modes = process.argv.includes('--smoke')
    ? ['baseline', 'typical']
    : ['baseline', 'off', 'typical', 'heavy'];
  const orders = process.argv.includes('--smoke')
    ? [modes]
    : [modes, [...modes].reverse(), ['typical', 'baseline', 'heavy', 'off']];
  const gameJar = path.join(repo, 'game/build/libs/space-client-0.1.0.jar'),
    jarBytes = await fs.readFile(gameJar),
    jarSha = createHash('sha256').update(jarBytes).digest('hex');
  const helperSha = createHash('sha256')
    .update(await fs.readFile(path.join(qa, 'native-perf.jar')))
    .digest('hex');
  const tree = execFileSync('git', ['rev-parse', 'HEAD:game'], {
    cwd: repo,
    encoding: 'utf8',
    windowsHide: true,
  }).trim();
  const samples = [];
  async function linkTree(from, to) {
    await fs.mkdir(to, { recursive: true });
    for (const e of await fs.readdir(from, { withFileTypes: true })) {
      const a = path.join(from, e.name),
        b = path.join(to, e.name);
      if (e.isDirectory()) await linkTree(a, b);
      else if (e.isFile()) await fs.link(a, b);
      else throw Error('Unexpected cache link');
    }
  }
  function memory(pid) {
    return JSON.parse(
      execFileSync(
        'powershell.exe',
        [
          '-NoProfile',
          '-NonInteractive',
          '-Command',
          `$p=Get-Process -Id ${pid} -ErrorAction Stop; [pscustomobject]@{workingSetBytes=$p.WorkingSet64;privateBytes=$p.PrivateMemorySize64;peakWorkingSetBytes=$p.PeakWorkingSet64}|ConvertTo-Json -Compress`,
        ],
        { windowsHide: true, encoding: 'utf8', timeout: 10000 },
      ),
    );
  }
  function otherJava() {
    return JSON.parse(
      execFileSync(
        'powershell.exe',
        [
          '-NoProfile',
          '-NonInteractive',
          '-Command',
          'ConvertTo-Json -Compress -InputObject @((Get-Process java -ErrorAction SilentlyContinue)|Select-Object -ExpandProperty Id)',
        ],
        { windowsHide: true, encoding: 'utf8', timeout: 10000 },
      ),
    );
  }
  async function trial(mode, repetition) {
    if (otherJava().length)
      throw Error('Another Java process is active; preserve a quiet measurement window.');
    const root = path.join(runRoot, `${repetition}-${mode}`);
    await fs.mkdir(root);
    for (const folder of ['libraries', 'assets', 'versions', 'natives'])
      await linkTree(path.join(cache, folder), path.join(root, folder));
    await fs.mkdir(path.join(root, 'mods'));
    if (mode !== 'baseline') await fs.copyFile(gameJar, path.join(root, 'mods/space-client.jar'));
    await fs.writeFile(
      path.join(root, 'options.txt'),
      'guiScale:2\npauseOnLostFocus:false\nmaxFps:260\nframerateLimit:260\nenableVsync:false\nrenderDistance:4\nfancyGraphics:false\nparticles:2\ndifficulty:0\nsoundCategory_master:0.0\nlang:en_US\n',
    );
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
    if (!natives) throw Error('Own native cache missing');
    const args = [
      '-Xms256M',
      '-Xmx1024M',
      '-Dfile.encoding=UTF-8',
      '-Dfabric.noGui=true',
      '-Djava.library.path=' + natives,
      '-Dfabric.gameVersion=1.8.9',
      '-Dfabric.gameJarPath=' + path.join(root, 'versions/1.8.9/1.8.9.jar'),
      '-Dfabric.addMods=' + path.join(qa, 'native-perf.jar'),
      '-Dspace.perf.mode=' + mode,
      '-Dspace.perf.tree=' + (mode === 'baseline' ? 'none' : tree),
      '-cp',
      plan.classpath.map((p) => path.join(root, p)).join(path.delimiter),
      plan.mainClass,
      '--username',
      'SpacePerfQA',
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
    const start = performance.now(),
      log = [];
    let titleMs,
      ready = false,
      failed = false,
      pending = '',
      working;
    child = spawn(path.join(repo, '.tools/jdk-21/bin/java.exe'), args, {
      cwd: root,
      env,
      windowsHide: true,
      stdio: ['ignore', 'pipe', 'pipe'],
    });
    const inspect = (chunk) => {
      const text = chunk.toString();
      log.push(text);
      pending = (pending + text).slice(-12000);
      if (titleMs === undefined && pending.includes('SPACE_PERF_TITLE_READY'))
        titleMs = performance.now() - start;
      if (!ready && pending.includes('SPACE_PERF_SAMPLE_READY')) {
        ready = true;
        try {
          working = memory(child.pid);
        } catch {
          failed = true;
        }
        void fs.writeFile(path.join(root, 'perf-stop.flag'), 'stop');
      }
    };
    child.stdout.on('data', inspect);
    child.stderr.on('data', inspect);
    const timeout = setTimeout(() => {
      failed = true;
      child.kill();
    }, 90000);
    const code = await new Promise((resolve, reject) => {
      child.once('error', reject);
      child.once('close', resolve);
    });
    clearTimeout(timeout);
    child = null;
    await fs.writeFile(path.join(root, 'native.log'), log.join(''));
    const failure = await fs
      .readFile(path.join(root, 'perf-failure.txt'), 'utf8')
      .catch(() => null);
    if (failure) throw Error(failure);
    if (code !== 0 || failed || !ready || titleMs === undefined)
      throw Error('Incomplete native performance sample: ' + root);
    const sample = JSON.parse(await fs.readFile(path.join(root, 'perf-result.json'), 'utf8'));
    if (!sample.ok || sample.mode !== mode || sample.spaceLoaded !== (mode !== 'baseline'))
      throw Error('Mode/source isolation failed');
    const expectedHud = { baseline: 0, off: 0, typical: 5, heavy: 26 }[mode];
    if (
      sample.enabledHud.length !== expectedHud ||
      sample.width !== 1280 ||
      sample.height !== 720 ||
      sample.maxFramerateReturn !== 260 ||
      sample.position.some((value, i) => Math.abs(value - [0.5, 4, 0.5, 0, 0][i]) > 0.001)
    )
      throw Error('Controlled scene/profile invariant failed');
    if (/\/(ERROR|FATAL)\]/.test(log.join(''))) throw Error('Native errors invalidate the sample');
    if (otherJava().length) throw Error('Another Java process overlapped the sample.');
    const result = {
      ...sample,
      repetition,
      titleFromJvmSpawnMs: titleMs,
      processMemoryAfterSample: working,
      log: path.relative(repo, path.join(root, 'native.log')),
    };
    await fs.writeFile(
      path.join(root, 'observations.json'),
      JSON.stringify(result, null, 2) + '\n',
    );
    samples.push(result);
    await fs.writeFile(path.join(runRoot, 'samples.json'), JSON.stringify(samples, null, 2) + '\n');
    console.log(
      JSON.stringify({
        mode,
        repetition,
        loopFps: sample.loopFps,
        p99IntervalMs: sample.intervalMs.p99,
        rssMiB: working.workingSetBytes / 1048576,
        titleMs,
      }),
    );
  }
  for (let repetition = 0; repetition < orders.length; repetition++)
    for (const mode of orders[repetition]) await trial(mode, repetition);
  const summary = modes.map((mode) => {
    const group = samples.filter((s) => s.mode === mode),
      median = (key) => {
        const a = group.map(key).sort((a, b) => a - b);
        return a[Math.floor(a.length / 2)];
      };
    return {
      mode,
      repetitions: group.length,
      medianFps: median((s) => s.loopFps),
      medianP99IntervalMs: median((s) => s.intervalMs.p99),
      medianRssMiB: median((s) => s.processMemoryAfterSample.workingSetBytes / 1048576),
      medianTitleMs: median((s) => s.titleFromJvmSpawnMs),
    };
  });
  await fs.writeFile(
    path.join(runRoot, 'report.json'),
    JSON.stringify(
      {
        ok: true,
        smoke: process.argv.includes('--smoke'),
        gameSourceTree: tree,
        jarSha256: jarSha,
        qaHelperSha256: helperSha,
        conditions: {
          baseline:
            'Minecraft + pinned Fabric + identical QA helper, without Space; not pure vanilla',
          warmupSeconds: 3,
          sampleSeconds: 6,
          resolution: '1280x720',
          renderDistance: 4,
          seed: 54321,
          level: 'flat/creative/peaceful',
          view: 'first person, fixed requested position/yaw, empty inventory',
          position: [0.5, 4, 0.5, 0, 0],
          timeOfDay: 1000,
          mobSpawning: false,
          vsync: false,
          maxFramerateReturn: 260,
          glFinish: true,
          gameplayModules: 'all disabled in every Space profile',
          wingsAndHalo: 'default none',
          ram: 'post-sample process working/private set, peak includes startup',
          startup: 'new JVM spawn to first rendered title, warm OS caches, not cold boot',
          scope: 'hidden Canvas observations, not ETW/presented frame qualification',
        },
        summary,
        samples,
      },
      null,
      2,
    ) + '\n',
  );
  await fs.writeFile(
    path.join(qa, 'latest-run.json'),
    JSON.stringify({ runRoot, report: path.join(runRoot, 'report.json') }),
  );
  console.log(JSON.stringify({ report: path.join(runRoot, 'report.json'), summary }));
} finally {
  child?.kill();
  await lock.close();
  await fs.unlink(lockPath);
}
