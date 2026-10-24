const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { spawnSync } = require('node:child_process');
const {
  parseCsv,
  stats,
  analyzeRun,
  summarizeSuite,
  TOOL_HASH,
} = require('../scripts/benchmark/analyze.cjs');
const header =
  'Application,ProcessID,SwapChainAddress,Runtime,TimeInSeconds,MsBetweenPresents,Dropped';
function fixture(variant = 'space-hud-on', repetition = 1, durationSeconds = 5) {
  const metadata = {
    schemaVersion: 1,
    status: 'complete',
    variant,
    repetition,
    processId: 42,
    durationSeconds,
    warmupSeconds: 30,
    startupMs: null,
    presentMon: { version: '2.3.1', sha256: TOOL_HASH },
    conditions: {
      schemaVersion: 1,
      sceneId: 'synthetic-test',
      route: 'fixed-camera',
      preset: 'low',
      javaVersion: '21.0.12',
      minecraftVersion: '1.8.9',
      worldSnapshotSha256: 'a'.repeat(64),
      optionsSha256: 'b'.repeat(64),
      width: 1280,
      height: 720,
      ramMiB: 3072,
      resourcePacks: [],
      vsync: false,
      fpsLimit: 0,
    },
    hardware: {
      cpu: ['Synthetic CPU'],
      gpu: [{ name: 'Synthetic GPU', driverVersion: 'test' }],
      ramBytes: 8 * 1024 ** 3,
      osBuild: 'test',
    },
    source: { commit: 'a'.repeat(40), gameSourceTree: 'b'.repeat(40) },
  };
  const csv =
    [
      header,
      ...Array.from(
        { length: durationSeconds * 100 + 1 },
        (_, i) => `java.exe,42,0,Other,${(1 + i / 100).toFixed(3)},${i ? 10 : 0},0`,
      ),
    ].join('\r\n') + '\r\n';
  const memory = Array.from({ length: durationSeconds * 2 + 1 }, (_, i) => ({
    elapsedMs: i * 500,
    workingSetBytes: 1000 + i,
    privateBytes: 900 + i,
  }));
  return { metadata, csv, memory };
}
const analyze = (f) => analyzeRun(f.metadata, f.csv, f.memory);
test('benchmark computes rates from intervals, keeps startup unknown and reports separate memory counters', () => {
  const r = analyze(fixture());
  assert.equal(r.averageFps, 100);
  assert.equal(r.onePercentLowFps, 100);
  assert.equal(r.frameMs.samples, 500);
  assert.equal(r.frameMs.p99, 10);
  assert.equal(r.startupMs, null);
  assert.equal(r.excludedInitialIntervals, 1);
  assert.equal(r.memory.workingSetBytes.max, 1010);
  assert.equal(r.memory.privateBytes.max, 910);
});
test('benchmark retains a long stall and uses mean of slowest one percent, not an FPS percentile', () => {
  const f = fixture();
  f.csv = f.csv.replace('6.000,10,0', '6.090,100,1');
  const r = analyze(f);
  assert.equal(r.frameMs.max, 100);
  assert.equal(r.frameMs.p99, 10);
  assert.ok(Math.abs(r.averageFps - 500000 / 5090) < 1e-9);
  assert.ok(Math.abs(r.onePercentLowFps - 1000 / 28) < 1e-9);
  assert.equal(r.droppedPresents, 1);
});
test('CSV parser handles BOM, CRLF, quoted commas, escaped quotes and legacy column casing', () => {
  const f = fixture();
  f.csv =
    '\uFEFF' +
    f.csv
      .replaceAll('MsBetweenPresents', 'msBetweenPresents')
      .replaceAll('java.exe', '"java, ""test"".exe"');
  assert.equal(parseCsv(f.csv)[0].application, 'java, "test".exe');
  assert.equal(analyze(f).averageFps, 100);
});
test('a complete very slow capture is retained with a small-sample warning', () => {
  const f = fixture();
  f.csv = [
    header,
    ...Array.from({ length: 6 }, (_, i) => `java.exe,42,0,Other,${i},${i ? 1000 : 0},0`),
  ].join('\n');
  const r = analyze(f);
  assert.equal(r.averageFps, 1);
  assert.equal(r.onePercentLowFps, 1);
  assert.ok(r.limitations.some((s) => s.includes('Fewer than 1000')));
});
test('CSV parser rejects truncated quotes, mismatched rows and duplicate headers', () => {
  for (const csv of [
    header + '\n"unterminated',
    header + '\njava.exe,42',
    header + ',ProcessID\njava.exe,42,0,Other,1,0,0,42',
  ]) {
    assert.throws(() => parseCsv(csv));
  }
});
test('capture cannot combine PIDs or multiple swap chains', () => {
  for (const replacement of [
    'java.exe,43,0,Other,6.000,10,0',
    'java.exe,42,0x1,Other,6.000,10,0',
  ]) {
    const f = fixture();
    f.csv = f.csv.replace('java.exe,42,0,Other,6.000,10,0', replacement);
    assert.throws(() => analyze(f), /another process|swap chain/);
  }
});
test('invalid frame values are rejected instead of discarded as outliers', () => {
  for (const value of ['', 'NaN', 'Infinity', '-1', '0']) {
    const f = fixture();
    f.csv = f.csv.replace('6.000,10,0', `6.000,${value},0`);
    assert.throws(() => analyze(f));
  }
  const f = fixture();
  f.csv = f.csv.replace('6.000,10,0', '5.990,10,0');
  assert.throws(() => analyze(f), /Non-monotonic/);
});
test('capture needs real duration, enough frames and a complete memory window', () => {
  const f = fixture();
  f.metadata.durationSeconds = 60;
  assert.throws(() => analyze(f), /Incomplete capture/);
  const sparse = fixture();
  sparse.csv = sparse.csv
    .split('\r\n')
    .filter((_, i) => i % 5 === 0)
    .join('\r\n');
  assert.throws(() => analyze(sparse), /timing coverage/);
  for (const samples of [
    [],
    [{ elapsedMs: 0, workingSetBytes: 1, privateBytes: 1 }],
    [
      { elapsedMs: 0, workingSetBytes: 1, privateBytes: 1 },
      { elapsedMs: 5000, workingSetBytes: 1, privateBytes: 1 },
    ],
  ]) {
    const m = fixture();
    m.memory = samples;
    assert.throws(() => analyze(m), /memory|Memory/);
  }
});
test('failed ETW, unverified tools, missing conditions and missing source cannot yield results', () => {
  for (const mutate of [
    (m) => (m.status = 'failed'),
    (m) => (m.presentMon.sha256 = 'wrong'),
    (m) => delete m.conditions.optionsSha256,
    (m) => (m.source.gameSourceTree = null),
    (m) => (m.startupMs = Infinity),
    (m) => (m.variant = 'unknown'),
  ]) {
    const f = fixture();
    mutate(f.metadata);
    assert.throws(() => analyze(f));
  }
});
function suite() {
  return ['vanilla', 'space-hud-off', 'space-hud-on'].flatMap((v) =>
    [1, 2, 3].map((i) => analyze(fixture(v, i, 60))),
  );
}
test('nine-run suite reports medians and range without pooling frames or inventing startup', () => {
  const rows = suite();
  rows[0].averageFps = 80;
  rows[1].averageFps = 120;
  const r = summarizeSuite(rows);
  assert.equal(r.vanilla.medianAverageFps, 100);
  assert.deepEqual(r.vanilla.averageFpsRange, [80, 120]);
  assert.deepEqual(r['space-hud-on'].runs, [1, 2, 3]);
  assert.equal(r.vanilla.medianStartupMs, null);
});
test('suite rejects duplicate runs and drift in scene, graphics, hardware, source, tool or timing', () => {
  assert.throws(() => summarizeSuite(suite().slice(1)), /three captures/);
  const duplicate = suite();
  duplicate[1] = duplicate[0];
  assert.throws(() => summarizeSuite(duplicate), /Duplicate/);
  for (const mutate of [
    (m) => (m.conditions.width = 1920),
    (m) => (m.conditions.worldSnapshotSha256 = 'c'.repeat(64)),
    (m) => (m.hardware.gpu[0].driverVersion = 'different'),
    (m) => (m.source.gameSourceTree = 'c'.repeat(40)),
    (m) => (m.presentMon.version = 'other'),
    (m) => (m.warmupSeconds = 0),
    (m) => (m.durationSeconds = 6),
  ]) {
    const rows = suite();
    mutate(rows[8].metadata);
    assert.throws(() => summarizeSuite(rows), /Different suite|Different Space/);
  }
});
test('nearest-rank percentile and even median definitions have known results', () => {
  const r = stats(Array.from({ length: 100 }, (_, i) => i + 1));
  assert.equal(r.mean, 50.5);
  assert.equal(r.median, 50.5);
  assert.equal(r.p95, 95);
  assert.equal(r.p99, 99);
  assert.equal(r.slowestOnePercentMean, 100);
});
test('CLI validates raw files, records hashes, refuses overwrite and never writes a failed result', (t) => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'space-benchmark-test-'));
  t.after(() => {
    const resolved = fs.realpathSync(root);
    assert.equal(path.dirname(resolved), fs.realpathSync(os.tmpdir()));
    assert.ok(path.basename(resolved).startsWith('space-benchmark-test-'));
    fs.rmSync(resolved, { recursive: true, force: true });
  });
  const f = fixture(),
    out = path.join(root, 'report.json');
  fs.writeFileSync(path.join(root, 'capture.json'), JSON.stringify(f.metadata));
  fs.writeFileSync(
    path.join(root, 'frames.csv'),
    Buffer.concat([Buffer.from([0xff, 0xfe]), Buffer.from(f.csv, 'utf16le')]),
  );
  fs.writeFileSync(path.join(root, 'memory.json'), JSON.stringify(f.memory));
  const cli = path.resolve(__dirname, '../scripts/benchmark/analyze.cjs');
  const run = () => spawnSync(process.execPath, [cli, root, '--out', out], { encoding: 'utf8' });
  assert.equal(run().status, 0);
  const report = JSON.parse(fs.readFileSync(out, 'utf8'));
  assert.equal(report.completeFrameComparison, false);
  assert.equal(report.allStartupMeasurementsPresent, false);
  assert.match(report.runs[0].rawSha256['frames.csv'], /^[a-f0-9]{64}$/);
  assert.notEqual(run().status, 0);
  fs.unlinkSync(out);
  f.metadata.status = 'failed';
  fs.writeFileSync(path.join(root, 'capture.json'), JSON.stringify(f.metadata));
  assert.notEqual(run().status, 0);
  assert.equal(fs.existsSync(out), false);
});
