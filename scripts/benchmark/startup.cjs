const fs = require('node:fs');
const path = require('node:path');
const { createHash } = require('node:crypto');
const { performance } = require('node:perf_hooks');
const { spawn, execFileSync } = require('node:child_process');
const { createInterface } = require('node:readline');

const repo = path.resolve(__dirname, '../..');
const hash = (bytes) => createHash('sha256').update(bytes).digest('hex');
const check = (condition, message) => {
  if (!condition) throw new Error(message);
};

// The timestamps bracket the observer call, not the game's presentation timestamp.
// No log line, window creation, spawn event or process age is a readiness signal.
function summarizeStartup(run) {
  check(run?.schemaVersion === 1, 'Invalid startup schema');
  check(
    run.failure === null && run.exit?.code === 0 && run.exit.signal === null,
    'Run did not exit normally',
  );
  const marks = ['requestedMs', 'preflightCompleteMs', 'spawnCallMs', 'spawnEventMs'];
  let previous = 0;
  for (const name of marks) {
    const value = run.marks[name];
    check(Number.isFinite(value) && value >= previous, 'Missing or unordered launch boundary');
    previous = value;
  }
  check(run.marks.requestedMs === 0, 'Request must define the clock origin');
  let end = run.marks.spawnEventMs;
  for (const observation of run.observations) {
    check(
      Number.isFinite(observation.beginMs) &&
        observation.beginMs >= end &&
        Number.isFinite(observation.endMs) &&
        observation.endMs >= observation.beginMs &&
        observation.endMs <= run.exit.observedMs &&
        /^[a-f0-9]{64}$/.test(observation.sha256),
      'Invalid or overlapping screenshot acquisition',
    );
    end = observation.endMs;
  }
  const ready = run.observations.findIndex((o) => o.review === 'main-menu');
  check(ready >= 0, 'No visually reviewed main menu');
  check(
    run.observations.slice(0, ready).every((o) => o.review === 'not-main-menu'),
    'Review every earlier screenshot',
  );
  const response = run.observations.find((o) => o.id === run.interaction?.responseObservationId);
  check(
    run.interaction?.fromObservationId === run.observations[ready].id &&
      response?.review === 'singleplayer-menu' &&
      response.beginMs >= run.observations[ready].endMs,
    'Menu interaction was not visually verified',
  );
  // A returned screenshot can still show the preceding screen after a transition.
  // Without a trusted frame timestamp a negative screenshot cannot tighten the lower bound.
  const lower = run.marks.spawnCallMs;
  const upper = run.observations[ready].endMs;
  const interval = (origin) => ({ lowerMs: lower - origin, upperMs: upper - origin });
  return {
    endpoint: 'visually-reviewed-main-menu',
    clock:
      'collector performance.now; observer calls bracketed over local stdin/stdout; no frame timestamp',
    preflightMs: run.marks.preflightCompleteMs,
    requestToSpawnCallMs: run.marks.spawnCallMs,
    spawnCallToEventMs: run.marks.spawnEventMs - run.marks.spawnCallMs,
    requestToMenuMs: interval(0),
    spawnCallToMenuMs: interval(run.marks.spawnCallMs),
    observationUncertaintyMs: upper - lower,
    lowerBoundSource: 'spawn call only; no trusted frame presentation timestamp',
    responseVerifiedLater: true,
    startupMs: null,
    worldReadyMeasured: false,
  };
}

function analyzeStartup(directory) {
  const run = JSON.parse(fs.readFileSync(path.join(directory, 'startup.json'), 'utf8'));
  for (const observation of run.observations) {
    check(/^observation-\d+\.(png|jpg)$/.test(observation.file), 'Invalid evidence filename');
    check(
      hash(fs.readFileSync(path.join(directory, observation.file))) === observation.sha256,
      'Screenshot changed',
    );
  }
  check(
    hash(fs.readFileSync(path.join(directory, 'java-output.log'))) === run.outputSha256,
    'Java output changed',
  );
  return summarizeStartup(run);
}

class StartupSession {
  constructor(directory, pin) {
    this.directory = directory;
    this.instance = path.join(directory, 'instance');
    this.pin = pin;
    this.child = null;
    this.run = null;
    this.sequence = 0;
  }
  save() {
    const target = path.join(this.runDirectory, 'startup.json');
    fs.writeFileSync(target + '.tmp', JSON.stringify(this.run, null, 2) + '\n');
    fs.renameSync(target + '.tmp', target);
  }
  elapsed() {
    return performance.now() - this.origin;
  }
  async start(label) {
    check(!this.child, 'Close the current game through its menu before another launch');
    check(!this.pending, 'Complete the pending observation before another launch');
    check(['diagnostic', 'repeat-1', 'repeat-2', 'repeat-3'].includes(label), 'Invalid run label');
    this.runDirectory = path.join(this.directory, label);
    fs.mkdirSync(this.runDirectory); // Refuse duplicate/replaced evidence.
    this.run = {
      schemaVersion: 1,
      label,
      mode: 'Space local development; HUD configured ON; main menu only',
      cacheRegime:
        'prepared files; preflight reads all artifacts; OS/GPU caches uncontrolled; not cold',
      optionsSha256: hash(fs.readFileSync(path.join(this.instance, 'options.txt'))),
      hudSha256: hash(
        fs.readFileSync(path.join(this.instance, 'config/spaceclient-hud.properties')),
      ),
      marks: { requestedMs: 0 },
      requestedUtc: new Date().toISOString(),
      observations: [],
      interaction: null,
      exit: null,
      failure: null,
    };
    const { prepareLocalLaunch } = require('../../apps/launcher/src/main/launch-plan.ts');
    this.origin = performance.now();
    try {
      const plan = await prepareLocalLaunch(
        this.instance,
        {
          id: 'default',
          name: 'Startup QA',
          memoryMb: 3072,
          javaPath: path.join(repo, '.tools/jdk-21/bin/java.exe'),
        },
        'SpaceTester',
        this.pin,
        new AbortController().signal,
      );
      this.run.marks.preflightCompleteMs = this.elapsed();
      // The regular launch plan is used unchanged, including 1000 x 680 and sanitized env.
      const output = fs.createWriteStream(path.join(this.runDirectory, 'java-output.log'), {
        flags: 'wx',
      });
      const run = this.run;
      this.run.marks.spawnCallMs = this.elapsed();
      const child = spawn(plan.executable, plan.args, {
        cwd: plan.cwd,
        env: plan.env,
        shell: false,
        windowsHide: false,
        stdio: ['ignore', 'pipe', 'pipe'],
      });
      this.child = child;
      child.stdout.on('data', (chunk) => output.write(chunk));
      child.stderr.on('data', (chunk) => output.write(chunk));
      output.on('error', () => {
        run.failure = 'Cannot preserve Java output';
      });
      child.once('error', () => {
        run.failure = 'Java spawn failed';
      });
      child.once('close', (code, signal) => {
        run.exit = { code, signal, observedMs: this.elapsed(), utc: new Date().toISOString() };
        output.end(() => {
          run.outputSha256 = hash(fs.readFileSync(path.join(this.runDirectory, 'java-output.log')));
          this.child = null;
          this.save();
        });
      });
      await new Promise((resolve, reject) => {
        child.once('spawn', resolve);
        child.once('error', () => reject(new Error('Java spawn failed')));
      });
      this.run.marks.spawnEventMs = this.elapsed();
      this.run.processId = child.pid;
      this.save();
      return { processId: child.pid, directory: this.runDirectory, marks: this.run.marks };
    } catch (error) {
      this.run.failure = 'Preflight or spawn failed';
      this.save();
      throw error;
    }
  }
  beginObservation() {
    check(this.child && !this.run.exit, 'No running game');
    check(!this.pending, 'Complete the pending observation first');
    this.pending = { id: ++this.sequence, beginMs: this.elapsed() };
    return this.pending;
  }
  endObservation(id, dataUrl) {
    const endMs = this.elapsed();
    check(this.pending?.id === id && this.child && !this.run.exit, 'No matching live observation');
    check(
      typeof dataUrl === 'string' && dataUrl.length <= 20 * 1024 ** 2,
      'Expected a bounded screenshot',
    );
    const match = /^data:image\/(png|jpeg);base64,/.exec(dataUrl);
    check(match, 'Expected a PNG or JPEG screenshot');
    const bytes = Buffer.from(dataUrl.slice(match[0].length), 'base64');
    const signature = match[1] === 'png' ? [137, 80, 78, 71, 13, 10, 26, 10] : [255, 216, 255];
    check(
      bytes.subarray(0, signature.length).equals(Buffer.from(signature)),
      'Invalid image signature',
    );
    const file = `observation-${id}.${match[1] === 'png' ? 'png' : 'jpg'}`;
    fs.writeFileSync(path.join(this.runDirectory, file), bytes, { flag: 'wx' });
    const observation = { ...this.pending, endMs, file, sha256: hash(bytes), review: 'unreviewed' };
    this.run.observations.push(observation);
    this.pending = null;
    this.save();
    return observation;
  }
  review(id, result) {
    check(
      ['not-main-menu', 'main-menu', 'singleplayer-menu'].includes(result),
      'Invalid visual review',
    );
    const observation = this.run.observations.find((o) => o.id === id);
    check(observation, 'Unknown observation');
    check(
      hash(fs.readFileSync(path.join(this.runDirectory, observation.file))) === observation.sha256,
      'Screenshot changed',
    );
    observation.review = result;
    this.save();
  }
  verifyInteraction(from, response) {
    this.run.interaction = { fromObservationId: from, responseObservationId: response };
    this.save();
  }
  summarize() {
    check(!this.child && !this.pending, 'Finish the game and pending observations first');
    this.save();
    const summary = analyzeStartup(this.runDirectory);
    fs.writeFileSync(
      path.join(this.runDirectory, 'summary.json'),
      JSON.stringify(summary, null, 2) + '\n',
      { flag: 'wx' },
    );
    return summary;
  }
}

async function createSession(source) {
  check(
    process.platform === 'win32',
    'This QA collector targets the existing Windows development runtime',
  );
  const reports = fs.realpathSync(path.join(repo, '.reports'));
  const sourceRoot = fs.realpathSync(path.resolve(repo, source));
  const relative = path.relative(reports, sourceRoot);
  check(
    relative && !relative.startsWith('..') && !path.isAbsolute(relative),
    'Use an existing isolated .reports QA instance',
  );
  const git = (...args) => execFileSync('git', args, { cwd: repo, encoding: 'utf8' }).trim();
  check(!git('status', '--porcelain', '--', 'game'), 'Commit game changes before measuring');
  const pin = JSON.parse(
    fs.readFileSync(path.join(repo, 'apps/launcher/dist/game/space-build.json')),
  );
  check(
    pin.gameSourceTree === git('rev-parse', 'HEAD:game'),
    'Build the matching launcher/game bundle',
  );
  fs.mkdirSync(path.join(reports, 'benchmark'), { recursive: true });
  const directory = fs.mkdtempSync(path.join(reports, 'benchmark/startup-'));
  const session = new StartupSession(directory, pin);
  fs.mkdirSync(session.instance);
  for (const folder of ['assets', 'libraries', 'versions', 'natives'])
    fs.cpSync(path.join(sourceRoot, folder), path.join(session.instance, folder), {
      recursive: true,
    });
  fs.mkdirSync(path.join(session.instance, 'mods'));
  fs.copyFileSync(
    path.join(sourceRoot, 'mods/space-client.jar'),
    path.join(session.instance, 'mods/space-client.jar'),
  );
  fs.mkdirSync(path.join(session.instance, 'config'));
  fs.writeFileSync(
    path.join(session.instance, 'config/spaceclient-hud.properties'),
    'schemaVersion=2\nactiveProfile=0\nprofile.0.enabled=true\nprofile.0.fps=true\nprofile.0.cps=true\nprofile.0.coordinates=true\nprofile.0.keystrokes=true\n',
  );
  fs.writeFileSync(
    path.join(session.instance, 'options.txt'),
    [
      'lang:en_US',
      'guiScale:2',
      'renderDistance:8',
      'maxFps:260',
      'enableVsync:false',
      'fancyGraphics:false',
      'particles:2',
      'mipmapLevels:0',
      'resourcePacks:[]',
      'incompatibleResourcePacks:[]',
      'forceUnicodeFont:false',
      'fullscreen:false',
      'overrideWidth:0',
      'overrideHeight:0',
      'mouseSensitivity:0.5',
      'pauseOnLostFocus:true',
      'soundCategory_music:0.0',
      '',
    ].join('\n'),
  );
  const sources = [
    'scripts/benchmark/startup.cjs',
    'scripts/benchmark/startup-observer.mjs',
    'apps/launcher/src/main/launch-plan.ts',
    'apps/launcher/src/main/java.ts',
  ];
  fs.copyFileSync(__filename, path.join(directory, 'collector-at-capture.cjs'));
  fs.copyFileSync(
    path.join(__dirname, 'startup-observer.mjs'),
    path.join(directory, 'observer-at-capture.mjs'),
  );
  fs.writeFileSync(
    path.join(directory, 'session.json'),
    JSON.stringify(
      {
        schemaVersion: 1,
        createdUtc: new Date().toISOString(),
        sourceCommit: git('rev-parse', 'HEAD'),
        pin,
        sourceHashes: Object.fromEntries(
          sources.map((file) => [file, hash(fs.readFileSync(path.join(repo, file)))]),
        ),
        node: process.version,
        platform: process.platform,
        memoryMb: 3072,
        width: 1000,
        height: 680,
        setupExcludedFromTiming:
          'Clone verified runtime into a fresh QA directory; generate options/HUD; import launch module',
        scope:
          'Local Space main-menu observation only; no world startup, vanilla comparison, download, Electron/UI/IPC timing or authenticated baseline',
      },
      null,
      2,
    ) + '\n',
  );
  return session;
}

async function main() {
  const session = await createSession(process.argv[2]);
  console.log(JSON.stringify({ event: 'ready', directory: session.directory }));
  const lines = createInterface({ input: process.stdin, crlfDelay: Infinity });
  for await (const line of lines) {
    let request;
    try {
      request = JSON.parse(line);
      let result;
      switch (request.command) {
        case 'start':
          result = await session.start(request.label);
          break;
        case 'begin':
          result = session.beginObservation();
          break;
        case 'end':
          result = session.endObservation(request.observationId, request.dataUrl);
          break;
        case 'review':
          session.review(request.observationId, request.result);
          break;
        case 'interaction':
          session.verifyInteraction(request.from, request.response);
          break;
        case 'status':
          result = { running: Boolean(session.child), run: session.run };
          break;
        case 'summary':
          result = session.summarize();
          break;
        default:
          throw new Error('Unknown QA command');
      }
      console.log(JSON.stringify({ id: request.id, result: result ?? null }));
    } catch (error) {
      console.log(JSON.stringify({ id: request?.id, error: error.message }));
    }
  }
  // Closing the observer never kills Java or a saving world. Child listeners keep us alive.
}
module.exports = { summarizeStartup, analyzeStartup, StartupSession };
if (require.main === module)
  if (process.argv[2] === '--analyze') {
    try {
      console.log(JSON.stringify(analyzeStartup(process.argv[3]), null, 2));
    } catch (error) {
      console.error(error.message);
      process.exitCode = 1;
    }
  } else {
    main().catch(() => {
      console.error('Startup QA initialization failed');
      process.exitCode = 1;
    });
  }
