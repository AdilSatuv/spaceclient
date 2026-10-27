const { test } = require('node:test');
const assert = require('node:assert/strict');

const status = (phase = 'idle', profileId = 'alpha') => ({
  profileId,
  mode: 'local',
  phase,
  message: '',
  exitCode: null,
  log: [],
});
const model = (changes = {}) => ({
  profile: { id: 'alpha' },
  snapshot: { localNickname: 'SpaceTester' },
  busy: false,
  dirty: false,
  nicknameDirty: false,
  launch: status(),
  ...changes,
});

test('launch availability follows the renderer gates and does not claim preflight success', async () => {
  const { launchAvailability } = await import('../src/renderer/model/launch-availability.ts');
  for (const phase of ['idle', 'exited', 'error']) {
    const result = launchAvailability(model({ launch: status(phase) }), false);
    assert.equal(result.disabled, false, phase);
    assert.match(result.reason, /будут проверены перед запуском/);
  }
  for (const phase of ['checking', 'starting', 'running']) {
    assert.equal(launchAvailability(model({ launch: status(phase) }), false).disabled, true, phase);
  }
  const cases = [
    [model(), true, /подготовки файлов/],
    [model({ busy: true }), false, /текущего действия/],
    [model({ dirty: true }), false, /изменения профиля/],
    [model({ nicknameDirty: true }), false, /ник и сохраните/],
    [model({ snapshot: { localNickname: null } }), false, /Введите и сохраните/],
    [model({ launch: null }), false, /перезапустите лаунчер/],
    [model({ launch: status('running', 'beta') }), false, /перезапустите лаунчер/],
  ];
  for (const [input, preparing, reason] of cases) {
    const result = launchAvailability(input, preparing);
    assert.equal(result.disabled, true);
    assert.match(result.reason, reason);
  }
});

test('explanations prioritize the active operation and unblock in actionable order', async () => {
  const { launchAvailability } = await import('../src/renderer/model/launch-availability.ts');
  const state = model({
    launch: status('running'),
    busy: true,
    dirty: true,
    nicknameDirty: true,
    snapshot: { localNickname: null },
  });
  assert.match(launchAvailability(state, true).reason, /уже работает/);
  state.launch = status();
  assert.match(launchAvailability(state, true).reason, /подготовки файлов/);
  assert.match(launchAvailability(state, false).reason, /текущего действия/);
  state.busy = false;
  assert.match(launchAvailability(state, false).reason, /изменения профиля/);
  state.dirty = false;
  assert.match(launchAvailability(state, false).reason, /ник и сохраните/);
  state.nicknameDirty = false;
  assert.match(launchAvailability(state, false).reason, /Введите и сохраните/);
  state.snapshot.localNickname = 'SpaceTester';
  assert.equal(launchAvailability(state, false).disabled, false);
});
