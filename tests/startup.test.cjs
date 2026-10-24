const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { summarizeStartup, StartupSession } = require('../scripts/benchmark/startup.cjs');

function fixture() {
  return {
    schemaVersion: 1,
    failure: null,
    marks: { requestedMs: 0, preflightCompleteMs: 100, spawnCallMs: 110, spawnEventMs: 115 },
    observations: [
      { id: 1, beginMs: 200, endMs: 300, sha256: 'a'.repeat(64), review: 'not-main-menu' },
      { id: 2, beginMs: 400, endMs: 500, sha256: 'b'.repeat(64), review: 'main-menu' },
      { id: 3, beginMs: 600, endMs: 700, sha256: 'c'.repeat(64), review: 'singleplayer-menu' },
    ],
    interaction: { fromObservationId: 2, responseObservationId: 3 },
    exit: { code: 0, signal: null, observedMs: 1000 },
  };
}
test('startup preserves an upper bound; stale negative images cannot imply an exact frame time', () => {
  const result = summarizeStartup(fixture());
  assert.equal(result.preflightMs, 100);
  assert.equal(result.requestToSpawnCallMs, 110);
  assert.equal(result.spawnCallToEventMs, 5);
  assert.deepEqual(result.requestToMenuMs, { lowerMs: 110, upperMs: 500 });
  assert.deepEqual(result.spawnCallToMenuMs, { lowerMs: 0, upperMs: 390 });
  assert.equal(result.observationUncertaintyMs, 390);
  assert.equal(result.startupMs, null);
  assert.equal(result.worldReadyMeasured, false);
});
test('first observation already at menu yields only a broad bound starting at spawn call', () => {
  const run = fixture();
  run.observations.shift();
  assert.deepEqual(summarizeStartup(run).spawnCallToMenuMs, { lowerMs: 0, upperMs: 390 });
});
test('missing/failed exits, log-only and old FPS captures cannot become startup measurements', () => {
  for (const modify of [
    (r) => {
      r.exit = null;
    },
    (r) => {
      r.exit.code = 1;
    },
    (r) => {
      r.exit.signal = 'SIGTERM';
    },
    (r) => {
      r.failure = 'preflight failed';
    },
    (r) => {
      r.observations = [];
      r.log = ['OpenAL initialized', 'Created: 512x512 textures-atlas'];
    },
    (r) => {
      r.observations[1].review = 'unreviewed';
    },
    (r) => {
      r.observations[0].review = 'unreviewed';
    },
    (r) => {
      r.interaction = null;
    },
    (r) => {
      r.observations[2].review = 'not-main-menu';
    },
  ]) {
    const run = fixture();
    modify(run);
    assert.throws(() => summarizeStartup(run));
  }
  assert.throws(() => summarizeStartup({ schemaVersion: 1, status: 'complete', startupMs: null }));
});
test('unordered, nonfinite, overlapping, post-exit and corrupt evidence is rejected', () => {
  for (const modify of [
    (r) => {
      r.marks.preflightCompleteMs = NaN;
    },
    (r) => {
      r.marks.spawnEventMs = 50;
    },
    (r) => {
      r.marks.requestedMs = 1;
    },
    (r) => {
      r.observations[1].beginMs = 250;
    },
    (r) => {
      r.observations[1].endMs = Infinity;
    },
    (r) => {
      r.observations[1].endMs = 350;
    },
    (r) => {
      r.observations[1].sha256 = 'fake';
    },
    (r) => {
      r.exit.observedMs = 650;
    },
  ]) {
    const run = fixture();
    modify(run);
    assert.throws(() => summarizeStartup(run));
  }
});
test('collector reserves live acquisitions, checks screenshot hashes and refuses evidence overwrite', (t) => {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'space-startup-test-'));
  assert.equal(path.dirname(directory), os.tmpdir());
  t.after(() => fs.rmSync(directory, { recursive: true, force: true }));
  const session = new StartupSession(directory, null);
  session.runDirectory = directory;
  session.run = fixture();
  session.run.observations = [];
  session.run.exit = null;
  session.origin = performance.now();
  assert.throws(() => session.beginObservation(), /No running/);
  session.child = {};
  const pending = session.beginObservation();
  assert.throws(() => session.beginObservation(), /pending/);
  assert.throws(() => session.endObservation(pending.id + 1, ''), /matching/);
  assert.throws(
    () => session.endObservation(pending.id, 'data:image/png;base64,YmFk'),
    /signature/,
  );
  // Synthetic PNG signature only; this is a collector validation test, not a real screenshot.
  const bytes = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);
  const observation = session.endObservation(
    pending.id,
    'data:image/png;base64,' + bytes.toString('base64'),
  );
  session.review(observation.id, 'main-menu');
  fs.appendFileSync(path.join(directory, observation.file), 'tampered');
  assert.throws(() => session.review(observation.id, 'not-main-menu'), /changed/);
  assert.throws(() => session.summarize(), /Finish/);
  session.child = null;
  session.run = fixture();
  const { createHash } = require('node:crypto');
  for (const entry of session.run.observations) {
    entry.file = 'observation-' + entry.id + '.jpg';
    fs.writeFileSync(path.join(directory, entry.file), 'synthetic-' + entry.id);
    entry.sha256 = createHash('sha256')
      .update('synthetic-' + entry.id)
      .digest('hex');
  }
  fs.writeFileSync(path.join(directory, 'java-output.log'), 'synthetic output');
  session.run.outputSha256 = createHash('sha256').update('synthetic output').digest('hex');
  session.summarize();
  assert.throws(() => session.summarize(), /EEXIST/);
  fs.appendFileSync(path.join(directory, session.run.observations[0].file), 'tampered');
  assert.throws(() => session.summarize(), /Screenshot changed/);
});

test('original JPEG screenshots are preserved without conversion', (t) => {
  const directory = fs.mkdtempSync(path.join(os.tmpdir(), 'space-startup-jpeg-'));
  assert.equal(path.dirname(directory), os.tmpdir());
  t.after(() => fs.rmSync(directory, { recursive: true, force: true }));
  const session = new StartupSession(directory, null);
  session.runDirectory = directory;
  session.run = { observations: [] };
  session.origin = performance.now();
  session.child = {};
  const pending = session.beginObservation();
  const bytes = Buffer.from([255, 216, 255, 224]);
  const result = session.endObservation(
    pending.id,
    'data:image/jpeg;base64,' + bytes.toString('base64'),
  );
  assert.match(result.file, /\.jpg$/);
  assert.deepEqual(fs.readFileSync(path.join(directory, result.file)), bytes);
});
