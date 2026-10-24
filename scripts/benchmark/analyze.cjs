const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const VARIANTS = ['vanilla', 'space-hud-off', 'space-hud-on'];
const TOOL_HASH = '364e5d98d4d134bd54dd25c22ed2ca2f4883f8bc3ed6502bee0c151e3436d30c';
const fail = (message) => {
  throw new Error(message);
};
const finite = (value, label, min = 0) => {
  if (typeof value !== 'number' || !Number.isFinite(value) || value < min) fail(`Invalid ${label}`);
  return value;
};
const sha256 = (bytes) => crypto.createHash('sha256').update(bytes).digest('hex');
function decodeText(bytes) {
  if (bytes[0] === 0xff && bytes[1] === 0xfe) {
    if (bytes.length % 2) fail('Truncated UTF-16 capture');
    return bytes.subarray(2).toString('utf16le');
  }
  if (bytes[0] === 0xfe && bytes[1] === 0xff) {
    if (bytes.length % 2) fail('Truncated UTF-16 capture');
    return Buffer.from(bytes.subarray(2)).swap16().toString('utf16le');
  }
  return bytes.toString('utf8').replace(/^\uFEFF/, '');
}
const canonical = (value) =>
  JSON.stringify(value, (_, v) =>
    v && typeof v === 'object' && !Array.isArray(v)
      ? Object.fromEntries(
          Object.keys(v)
            .sort()
            .map((k) => [k, v[k]]),
        )
      : v,
  );

// RFC 4180 quoting; malformed/truncated rows must never become a successful benchmark.
function parseCsv(text) {
  text = text.replace(/^\uFEFF/, '');
  const rows = [];
  let row = [],
    field = '',
    quoted = false,
    closed = false;
  for (let i = 0; i < text.length; i++) {
    const c = text[i];
    if (quoted) {
      if (c === '"' && text[i + 1] === '"') {
        field += '"';
        i++;
      } else if (c === '"') {
        quoted = false;
        closed = true;
      } else field += c;
    } else if (c === '"') {
      if (field || closed) fail('Malformed CSV quote');
      quoted = true;
    } else if (c === ',' || c === '\n' || c === '\r') {
      row.push(field);
      field = '';
      closed = false;
      if (c !== ',') {
        if (c === '\r' && text[i + 1] === '\n') i++;
        if (row.some((v) => v !== '')) rows.push(row);
        row = [];
      }
    } else {
      if (closed) fail('Unexpected text after CSV quote');
      field += c;
    }
  }
  if (quoted) fail('Truncated CSV quote');
  if (field || closed || row.length) {
    row.push(field);
    rows.push(row);
  }
  if (rows.length < 2) fail('No frame rows');
  const headers = rows.shift().map((h) => h.toLowerCase());
  if (new Set(headers).size !== headers.length) fail('Duplicate CSV columns');
  for (const key of [
    'processid',
    'swapchainaddress',
    'runtime',
    'timeinseconds',
    'msbetweenpresents',
    'dropped',
  ]) {
    if (!headers.includes(key)) fail(`Missing CSV column ${key}`);
  }
  return rows.map((r) => {
    if (r.length !== headers.length) fail('CSV row length mismatch');
    return Object.fromEntries(headers.map((key, i) => [key, r[i]]));
  });
}
function validateConditions(c) {
  if (!c || c.schemaVersion !== 1) fail('Invalid conditions schema');
  for (const key of ['sceneId', 'route', 'preset', 'javaVersion', 'minecraftVersion']) {
    if (typeof c[key] !== 'string' || !c[key].trim() || c[key].length > 200)
      fail(`Missing condition ${key}`);
  }
  for (const key of ['worldSnapshotSha256', 'optionsSha256']) {
    if (!/^[a-f0-9]{64}$/.test(c[key])) fail(`Invalid condition ${key}`);
  }
  for (const key of ['width', 'height', 'ramMiB'])
    if (!Number.isSafeInteger(c[key]) || c[key] < 1) fail(`Invalid condition ${key}`);
  if (!Array.isArray(c.resourcePacks) || c.resourcePacks.some((p) => !/^[a-f0-9]{64}$/.test(p)))
    fail('Invalid resource pack hashes');
  if (
    typeof c.vsync !== 'boolean' ||
    typeof c.fpsLimit !== 'number' ||
    !Number.isFinite(c.fpsLimit) ||
    c.fpsLimit < 0
  )
    fail('Missing frame cap conditions');
  return c;
}
function stats(values) {
  if (!values.length) fail('No samples');
  values.forEach((v) => finite(v, 'sample', Number.MIN_VALUE));
  const ordered = [...values].sort((a, b) => a - b);
  const mean = values.reduce((a, b) => a + b, 0) / values.length;
  const percentile = (p) => ordered[Math.ceil(p * ordered.length) - 1];
  const tail = ordered.slice(-Math.ceil(ordered.length * 0.01));
  return {
    samples: values.length,
    mean,
    median:
      ordered.length % 2
        ? ordered[(ordered.length - 1) / 2]
        : (ordered[ordered.length / 2 - 1] + ordered[ordered.length / 2]) / 2,
    p95: percentile(0.95),
    p99: percentile(0.99),
    max: ordered.at(-1),
    slowestOnePercentMean: tail.reduce((a, b) => a + b, 0) / tail.length,
  };
}
function analyzeRun(metadata, csv, memory) {
  if (metadata.schemaVersion !== 1 || metadata.status !== 'complete')
    fail('Capture is not complete');
  if (!VARIANTS.includes(metadata.variant) || ![1, 2, 3].includes(metadata.repetition))
    fail('Invalid run identity');
  if (metadata.presentMon?.version !== '2.3.1' || metadata.presentMon.sha256 !== TOOL_HASH)
    fail('Unverified capture tool');
  finite(metadata.durationSeconds, 'capture duration', 5);
  finite(metadata.warmupSeconds, 'warmup duration');
  if (!Number.isSafeInteger(metadata.processId) || metadata.processId < 1)
    fail('Invalid process ID');
  validateConditions(metadata.conditions);
  if (
    !metadata.hardware?.cpu?.length ||
    !metadata.hardware?.gpu?.length ||
    !metadata.hardware.osBuild
  )
    fail('Missing machine conditions');
  finite(metadata.hardware.ramBytes, 'machine RAM', 1);
  if (!/^[a-f0-9]{40}$/.test(metadata.source?.commit)) fail('Missing source commit');
  if (metadata.variant !== 'vanilla' && !/^[a-f0-9]{40}$/.test(metadata.source.gameSourceTree))
    fail('Missing game source tree');
  if (metadata.startupMs !== null) finite(metadata.startupMs, 'startup duration', Number.MIN_VALUE);
  const frames = parseCsv(csv);
  const chains = new Set();
  let previous = -1,
    first = null,
    dropped = 0,
    excludedInitialIntervals = 0;
  const durations = [];
  for (const [i, frame] of frames.entries()) {
    if (frame.processid.trim() !== String(metadata.processId)) fail('CSV contains another process');
    chains.add(frame.swapchainaddress.toLowerCase());
    if (!frame.swapchainaddress || chains.size > 1)
      fail('Capture has multiple or missing swap chains');
    if (!frame.timeinseconds.trim() || !frame.msbetweenpresents.trim())
      fail('Missing frame timing');
    const time = finite(Number(frame.timeinseconds), 'frame timestamp');
    if (time <= previous) fail('Non-monotonic frame timestamps');
    first ??= time;
    previous = time;
    const ms = finite(Number(frame.msbetweenpresents), 'frame interval');
    if (!['0', '1'].includes(frame.dropped)) fail('Invalid dropped flag');
    dropped += Number(frame.dropped);
    // The first interval may begin before capture; every subsequent stall is retained.
    if (i === 0) {
      excludedInitialIntervals++;
      continue;
    }
    if (ms === 0) fail('Zero frame interval inside capture');
    durations.push(ms);
  }
  const observedSeconds = previous - first;
  if (
    durations.length < 2 ||
    observedSeconds < metadata.durationSeconds * 0.9 ||
    observedSeconds > metadata.durationSeconds * 1.1 + 1
  )
    fail('Incomplete capture duration or frame count');
  const coveredMs = durations.reduce((a, b) => a + b, 0);
  if (Math.abs(coveredMs - observedSeconds * 1000) > Math.max(50, observedSeconds * 20))
    fail('Incomplete frame timing coverage');
  const frameMs = stats(durations);
  if (!Array.isArray(memory) || memory.length < 2) fail('Missing process memory samples');
  let lastMemoryTime = -1;
  for (const sample of memory) {
    finite(sample.elapsedMs, 'memory timestamp');
    if (sample.elapsedMs <= lastMemoryTime) fail('Non-monotonic memory samples');
    if (lastMemoryTime >= 0 && sample.elapsedMs - lastMemoryTime > 1500)
      fail('Memory sampling gap exceeds 1500 ms');
    lastMemoryTime = sample.elapsedMs;
    finite(sample.workingSetBytes, 'working set', 1);
    finite(sample.privateBytes, 'private bytes', 1);
  }
  if (memory[0].elapsedMs > 1000 || lastMemoryTime > metadata.durationSeconds * 1100 + 2000)
    fail('Memory window does not match capture');
  if (lastMemoryTime - memory[0].elapsedMs < metadata.durationSeconds * 900)
    fail('Incomplete memory duration');
  return {
    metadata,
    observedSeconds,
    excludedInitialIntervals,
    droppedPresents: dropped,
    frameMs,
    averageFps: 1000 / frameMs.mean,
    onePercentLowFps: 1000 / frameMs.slowestOnePercentMean,
    memory: {
      workingSetBytes: stats(memory.map((m) => m.workingSetBytes)),
      privateBytes: stats(memory.map((m) => m.privateBytes)),
    },
    startupMs: metadata.startupMs,
    limitations: [
      'Present intervals, not displayed FPS or input latency.',
      'OpenGL Runtime=Other has reduced instrumentation.',
      ...(durations.length < 1000
        ? ['Fewer than 1000 intervals; the slowest 1% contains fewer than ten samples.']
        : []),
      ...(metadata.startupMs === null
        ? ['Startup was not measured; process age is not startup latency.']
        : []),
    ],
  };
}
function summarizeSuite(runs) {
  if (runs.length !== 9) fail('Comparison requires three captures of each variant');
  const baseline = runs[0].metadata;
  if (baseline.durationSeconds < 60 || baseline.warmupSeconds < 30)
    fail('Suite requires at least 30 seconds warmup and 60 seconds capture');
  const ids = new Set();
  let gameTree;
  for (const run of runs) {
    const m = run.metadata,
      id = `${m.variant}:${m.repetition}`;
    if (ids.has(id)) fail('Duplicate suite run');
    ids.add(id);
    for (const field of [
      'conditions',
      'hardware',
      'durationSeconds',
      'warmupSeconds',
      'presentMon',
    ]) {
      if (canonical(m[field]) !== canonical(baseline[field])) fail(`Different suite ${field}`);
    }
    if (m.variant !== 'vanilla') {
      gameTree ??= m.source.gameSourceTree;
      if (gameTree !== m.source.gameSourceTree) fail('Different Space game sources');
    }
  }
  return Object.fromEntries(
    VARIANTS.map((variant) => {
      const selected = runs.filter((r) => r.metadata.variant === variant);
      if (selected.length !== 3) fail('Missing suite variant');
      return [
        variant,
        {
          runs: selected.map((r) => r.metadata.repetition).sort(),
          medianAverageFps: stats(selected.map((r) => r.averageFps)).median,
          medianOnePercentLowFps: stats(selected.map((r) => r.onePercentLowFps)).median,
          averageFpsRange: [
            Math.min(...selected.map((r) => r.averageFps)),
            Math.max(...selected.map((r) => r.averageFps)),
          ],
          medianMaxSampledPrivateBytes: stats(selected.map((r) => r.memory.privateBytes.max))
            .median,
          medianStartupMs: selected.every((r) => r.startupMs !== null)
            ? stats(selected.map((r) => r.startupMs)).median
            : null,
        },
      ];
    }),
  );
}
function readRun(directory) {
  const bytes = {};
  const metadataPath = path.join(directory, 'capture.json');
  if (fs.statSync(metadataPath).size > 1024 * 1024) fail('Capture metadata exceeds 1 MiB');
  const metadata = JSON.parse(decodeText(fs.readFileSync(metadataPath)));
  if (metadata.status !== 'complete') fail('Capture is not complete');
  for (const file of ['capture.json', 'frames.csv', 'memory.json']) {
    const p = path.join(directory, file);
    if (fs.statSync(p).size > 128 * 1024 * 1024) fail('Capture file exceeds 128 MiB');
    bytes[file] = fs.readFileSync(p);
  }
  const run = analyzeRun(
    metadata,
    decodeText(bytes['frames.csv']),
    JSON.parse(decodeText(bytes['memory.json'])),
  );
  return {
    ...run,
    rawSha256: Object.fromEntries(Object.entries(bytes).map(([k, v]) => [k, sha256(v)])),
  };
}
if (require.main === module) {
  try {
    const args = process.argv.slice(2),
      outIndex = args.indexOf('--out');
    if (outIndex < 1 || outIndex !== args.length - 2)
      fail(
        'Usage: node scripts/benchmark/analyze.cjs <capture-dir> [eight-more-dirs] --out <new-report.json>',
      );
    const runs = args.slice(0, outIndex).map(readRun);
    if (![1, 9].includes(runs.length))
      fail('Use one diagnostic capture or a complete nine-run suite');
    const report = {
      schemaVersion: 1,
      metricDefinition:
        'average=1000/mean(interval); 1% low=1000/mean(slowest ceil(N*0.01) intervals); nearest-rank p95/p99; medians across runs',
      completeFrameComparison: runs.length === 9,
      allStartupMeasurementsPresent: runs.every((r) => r.startupMs !== null),
      variants: runs.length === 9 ? summarizeSuite(runs) : null,
      runs,
    };
    fs.writeFileSync(args[outIndex + 1], JSON.stringify(report, null, 2) + '\n', { flag: 'wx' });
    console.log(
      `Validated ${runs.length} capture(s); complete frame comparison: ${report.completeFrameComparison}`,
    );
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}
module.exports = {
  parseCsv,
  validateConditions,
  stats,
  analyzeRun,
  summarizeSuite,
  readRun,
  TOOL_HASH,
};
