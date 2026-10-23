const { test } = require('node:test');
const assert = require('node:assert/strict');
const ok = (value) => ({ ok: true, value });
function deferred() {
  let resolve;
  const promise = new Promise((done) => {
    resolve = done;
  });
  return { promise, resolve };
}
async function fixture() {
  const { LauncherSession } = await import('../src/renderer/model/launcher-session.ts');
  const snapshot = {
    schemaVersion: 1,
    activeProfileId: 'alpha',
    maxMemoryMb: 8192,
    totalMemoryMb: 16384,
    dataRoot: '/test',
    warning: null,
    profiles: ['alpha', 'beta'].map((id) => ({ id, name: id, memoryMb: 2048, javaPath: null })),
  };
  const calls = [];
  const api = {
    getState: async () => {
      calls.push('load');
      return ok(snapshot);
    },
    saveProfile: async (edit) => {
      calls.push(['save', edit]);
      Object.assign(
        snapshot.profiles.find((item) => item.id === edit.id),
        edit,
      );
      return ok(snapshot);
    },
    createProfile: async () => {
      calls.push('create');
      return ok(snapshot);
    },
    selectProfile: async (id) => {
      calls.push(['select', id]);
      snapshot.activeProfileId = id;
      return ok(snapshot);
    },
    chooseJava: async (id) => {
      calls.push(['choose', id]);
      return ok(snapshot);
    },
    checkJava: async (id) => {
      calls.push(['check', id]);
      return ok({ usable: true, major: 21, bits: 64, message: 'Checked' });
    },
    openInstance: async (id) => {
      calls.push(['open', id]);
      return ok(null);
    },
  };
  const session = new LauncherSession(api);
  await session.load();
  calls.length = 0;
  return { session, api, snapshot, calls };
}

test('session snapshots are stable between updates and detached from transport objects', async () => {
  const { session, snapshot } = await fixture();
  const initial = session.getSnapshot();
  assert.equal(initial, session.getSnapshot());
  snapshot.profiles[0].name = 'External mutation';
  assert.equal(initial.profile.name, 'alpha');
  let updates = 0;
  const unsubscribe = session.subscribe(() => updates++);
  session.editDraft({ name: 'Orbit' });
  assert.equal(updates, 1);
  assert.notEqual(initial, session.getSnapshot());
  assert.equal(initial.draft.name, 'alpha');
  unsubscribe();
  session.editDraft({ name: 'Next' });
  assert.equal(updates, 1);
});

test('dirty drafts block destructive navigation and choosing Java until saved or cancelled', async () => {
  const { session, calls } = await fixture();
  session.editDraft({ name: 'Unsaved', memoryMb: 4096 });
  for (const action of [
    session.load,
    session.createProfile,
    () => session.selectProfile('beta'),
    session.chooseJava,
  ]) {
    assert.equal(await action(), false);
    assert.equal(session.getSnapshot().draft.name, 'Unsaved');
    assert.match(session.getSnapshot().error, /сохраните/);
  }
  assert.equal(await session.selectProfile('alpha'), false);
  assert.deepEqual(calls, []);
  session.cancelEdit();
  assert.equal(session.getSnapshot().dirty, false);
  assert.equal(session.getSnapshot().error, '');
  assert.equal(session.getSnapshot().draft.name, 'alpha');
  assert.equal(await session.selectProfile('beta'), true);
  assert.equal(session.getSnapshot().profile.id, 'beta');
});

test('a pending save locks commands and edits; a failed save preserves the draft and can retry', async () => {
  const { session, api, calls } = await fixture();
  const pending = deferred();
  const save = api.saveProfile;
  api.saveProfile = (edit) => {
    calls.push(['pending', edit]);
    return pending.promise;
  };
  session.editDraft({ name: 'Orbit', memoryMb: 4096 });
  const first = session.saveProfile();
  assert.equal(session.getSnapshot().busy, true);
  assert.equal(await session.saveProfile(), false);
  assert.equal(await session.selectProfile('beta'), false);
  assert.equal(await session.checkJava(), false);
  session.editDraft({ name: 'Overwritten' });
  session.cancelEdit();
  assert.equal(calls.length, 1);
  assert.equal(session.getSnapshot().draft.name, 'Orbit');
  pending.resolve({ ok: false, error: 'Проверьте имя профиля.' });
  assert.equal(await first, false);
  assert.equal(session.getSnapshot().busy, false);
  assert.equal(session.getSnapshot().dirty, true);
  assert.equal(session.getSnapshot().error, 'Проверьте имя профиля.');
  api.saveProfile = save;
  assert.equal(await session.saveProfile(), true);
  assert.equal(session.getSnapshot().dirty, false);
  assert.equal(session.getSnapshot().profile.name, 'Orbit');
});

test('transport exceptions are redacted and invalid snapshots cannot replace valid state', async () => {
  const { session, api } = await fixture();
  const before = session.getSnapshot().snapshot;
  api.getState = async () => {
    throw new Error('C:/private/token=secret');
  };
  assert.equal(await session.load(), false);
  assert.match(session.getSnapshot().error, /связаться/);
  assert.doesNotMatch(session.getSnapshot().error, /private|secret/);
  api.getState = async () => ok({ ...before, activeProfileId: 'absent' });
  assert.equal(await session.load(), false);
  assert.equal(session.getSnapshot().snapshot, before);
  assert.equal(session.getSnapshot().busy, false);
});

test('Java checks stay with their profile and choosing Java clears only that profile cache', async () => {
  const { session, calls } = await fixture();
  await session.checkJava();
  assert.equal(session.getSnapshot().javaLabel, 'Проверена · 21 x64');
  await session.selectProfile('beta');
  assert.equal(session.getSnapshot().javaLabel, 'Не выбрана');
  await session.checkJava();
  await session.chooseJava();
  assert.deepEqual(Object.keys(session.getSnapshot().java), ['alpha']);
  await session.selectProfile('alpha');
  assert.equal(session.getSnapshot().javaLabel, 'Проверена · 21 x64');
  await session.openFolder();
  assert.deepEqual(calls.at(-1), ['open', 'alpha']);
});

test('an observer failure cannot turn a persisted save into a failed command', async () => {
  const { session } = await fixture();
  session.subscribe(() => {
    throw new Error('Broken view');
  });
  session.editDraft({ name: 'Saved' });
  assert.equal(await session.saveProfile(), true);
  assert.equal(session.getSnapshot().profile.name, 'Saved');
  assert.equal(session.getSnapshot().error, '');
});
