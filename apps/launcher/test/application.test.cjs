const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
async function fixture(t) {
  const { ProfileStore } = await import('../src/main/store.ts');
  const { LauncherApplication } = await import('../src/main/application/launcher.ts');
  const { createDispatcher } = await import('../src/main/application/dispatch.ts');
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'space-application-'));
  assert.equal(path.dirname(root), os.tmpdir());
  t.after(() => fs.rm(root, { recursive: true, force: true }));
  const profiles = await ProfileStore.open(root, 8192);
  const calls = [];
  const status = { profileId: 'default', phase: 'idle' };
  const dependencies = {
    profiles,
    installation: {
      running: false,
      status: (id) => {
        calls.push(['status', id]);
        return { ...status, profileId: id };
      },
      start: (id) => {
        calls.push(['start', id]);
        return { ...status, profileId: id };
      },
      cancel: async (id) => {
        calls.push(['cancel', id]);
        return { ...status, profileId: id };
      },
      shutdown: async () => {
        calls.push(['shutdown']);
      },
    },
    launch: {
      running: false,
      status: (id) => ({ profileId: id, phase: 'idle' }),
      start: (profile, name) => {
        calls.push(['launch', profile.id, name]);
        return { profileId: profile.id, phase: 'checking' };
      },
      shutdown: async () => {},
    },
    desktop: {
      chooseJava: async () => null,
      openDirectory: async (value) => {
        calls.push(['open', value]);
      },
    },
    inspectJava: async (value) => {
      calls.push(['java', value]);
      return { usable: true, major: 21, bits: 64, message: 'Checked' };
    },
  };
  const application = new LauncherApplication(dependencies);
  const event = {};
  const dispatch = createDispatcher(application.commands, (input) => input === event);
  return {
    root,
    profiles,
    application,
    dependencies,
    calls,
    dispatch,
    event,
    LauncherApplication,
    createDispatcher,
  };
}

test('application owns all 26 explicit commands and preserves profile persistence without Electron', async (t) => {
  const { application, dispatch, event, profiles } = await fixture(t);
  const { channels } = await import('../src/shared/ipc.ts');
  assert.equal(new Set(Object.values(channels)).size, 26);
  assert.deepEqual(Object.keys(application.commands).sort(), Object.values(channels).sort());
  assert.equal((await dispatch('state:get', event)).value.activeProfileId, 'default');
  const created = await dispatch('profile:create', event);
  assert.equal(created.ok, true);
  assert.equal(created.value.profiles.length, 2);
  const saved = await dispatch('profile:save', event, {
    id: 'default',
    name: 'Orbit',
    memoryMb: 3072,
  });
  assert.equal(saved.ok, true);
  const selected = await dispatch('profile:select', event, 'default');
  assert.equal(selected.value.activeProfileId, 'default');
  assert.equal(profiles.profile('default').memoryMb, 3072);
});
test('account payloads, confirmation and known profiles are checked before privileged account/recovery ports', async (t) => {
  const f = await fixture(t);
  let calls = 0;
  f.dependencies.account = {
    register: () => {
      calls++;
    },
    login: () => {
      calls++;
    },
    upload: () => {
      calls++;
    },
    apply: () => {
      calls++;
    },
    remove: () => {
      calls++;
    },
  };
  f.dependencies.recovery = {
    restore: () => {
      calls++;
    },
    backup: () => {
      calls++;
    },
  };
  for (const [command, payload] of [
    ['account:login', { username: 'abc', password: 'short' }],
    [
      'account:register',
      { username: 'abc', password: 'long password 123', nickname: 'ValidNick', token: 'x' },
    ],
    ['account:skin:upload', { expectedRevision: 1, model: 'classic', path: 'C:/secret' }],
    ['account:cape:upload', { expectedRevision: 1, model: 'slim' }],
    ['account:apply', { id: 'default', confirmIdentityChange: false }],
    ['profile:restore', { id: 'default', confirmed: false }],
    ['profile:backup', '../saves'],
    ['account:skin:delete', 0],
  ])
    assert.equal((await f.dispatch(command, f.event, payload)).ok, false, command);
  assert.equal(calls, 0);
});
test('maintenance reserves the instance until completion; running game blocks backup, restore and account apply', async (t) => {
  const f = await fixture(t);
  let release;
  const waiting = new Promise((resolve) => {
    release = resolve;
  });
  f.dependencies.recovery = {
    backup: async () => {
      await waiting;
      return { message: 'Saved', files: 6 };
    },
  };
  const backup = f.dispatch('profile:backup', f.event, 'default');
  await new Promise((resolve) => setImmediate(resolve));
  assert.equal(f.application.running, true);
  for (const [command, payload] of [
    ['local:save', 'OtherPlayer'],
    ['profile:save', { id: 'default', name: 'Changed', memoryMb: 2048 }],
    ['profile:create', undefined],
    ['profile:select', 'default'],
    ['java:choose', 'default'],
  ])
    assert.equal(
      (await f.dispatch(command, f.event, payload)).ok,
      false,
      command + ' must not race maintenance',
    );
  assert.equal(
    (await f.dispatch('state:get', f.event)).ok,
    true,
    'read-only state remains available',
  );
  assert.equal((await f.dispatch('launch:start', f.event, 'default')).ok, false);
  assert.equal((await f.dispatch('installation:start', f.event, 'default')).ok, false);
  release();
  assert.equal((await backup).ok, true);
  f.dependencies.launch.running = true;
  for (const [command, payload] of [
    ['profile:backup', 'default'],
    ['profile:restore', { id: 'default', confirmed: true }],
    ['account:apply', { id: 'default', confirmIdentityChange: true }],
  ])
    assert.equal((await f.dispatch(command, f.event, payload)).ok, false);
});
test('restore and explicit account Apply reject competing nickname/profile mutations while read-only state stays available', async (t) => {
  for (const kind of ['restore', 'apply']) {
    const f = await fixture(t);
    let release;
    const waiting = new Promise((resolve) => {
      release = resolve;
    });
    f.dependencies.recovery = {
      restore: async () => {
        await waiting;
        return { message: 'Restored', files: 6 };
      },
    };
    f.dependencies.account = {
      running: false,
      apply: async () => {
        await waiting;
        return {
          profile: null,
          expiresAt: null,
          skinPreview: null,
          capePreview: null,
          message: 'Applied',
        };
      },
    };
    const pending = f.dispatch(
      kind === 'restore' ? 'profile:restore' : 'account:apply',
      f.event,
      kind === 'restore'
        ? { id: 'default', confirmed: true }
        : { id: 'default', confirmIdentityChange: true },
    );
    await new Promise((resolve) => setImmediate(resolve));
    assert.equal((await f.dispatch('local:save', f.event, 'RacePlayer')).ok, false);
    assert.equal(
      (await f.dispatch('profile:save', f.event, { id: 'default', name: 'Race', memoryMb: 3072 }))
        .ok,
      false,
    );
    assert.equal((await f.dispatch('state:get', f.event)).ok, true);
    release();
    assert.equal((await pending).ok, true);
  }
});

test('unknown profiles and inputless command payloads fail before privileged ports are called', async (t) => {
  const { dispatch, event, calls } = await fixture(t);
  for (const channel of [
    'launch:get',
    'launch:start',
    'installation:get',
    'installation:start',
    'installation:cancel',
    'profile:open',
    'java:check',
    'java:choose',
  ]) {
    assert.equal((await dispatch(channel, event, '../outside')).ok, false);
    assert.equal((await dispatch(channel, event, 'unknown')).ok, false);
  }
  for (const channel of ['state:get', 'profile:create'])
    assert.equal((await dispatch(channel, event, { unwanted: true })).ok, false);
  assert.deepEqual(calls, []);
});

test('picker cancellation preserves settings; a renderer cannot choose the Java path through save', async (t) => {
  const { dispatch, event, dependencies, profiles } = await fixture(t);
  const original = profiles.snapshot();
  assert.deepEqual((await dispatch('java:choose', event, 'default')).value, original);
  assert.equal(
    (
      await dispatch('profile:save', event, {
        id: 'default',
        name: 'Injected',
        memoryMb: 2048,
        javaPath: 'C:\\attack\\java.exe',
      })
    ).ok,
    false,
  );
  dependencies.desktop.chooseJava = async () => 'C:\\owned-picker\\java.exe';
  assert.equal((await dispatch('java:choose', event, 'default')).ok, true);
  assert.equal(profiles.profile('default').javaPath, 'C:\\owned-picker\\java.exe');
  dependencies.desktop.chooseJava = async () => 'https://invalid/java.exe';
  assert.equal((await dispatch('java:choose', event, 'default')).ok, false);
  assert.equal(profiles.profile('default').javaPath, 'C:\\owned-picker\\java.exe');
});

test('Java picker/check guards include asynchronous work and recover after rejected ports', async (t) => {
  const { dispatch, event, dependencies } = await fixture(t);
  let rejectPicker;
  dependencies.desktop.chooseJava = () =>
    new Promise((_resolve, reject) => {
      rejectPicker = reject;
    });
  const first = dispatch('java:choose', event, 'default');
  assert.match((await dispatch('java:choose', event, 'default')).error, /уже открыт/);
  rejectPicker(new Error('PRIVATE-OS-PATH-OR-TOKEN'));
  assert.equal((await first).error.includes('PRIVATE'), false);
  dependencies.desktop.chooseJava = async () => null;
  assert.equal((await dispatch('java:choose', event, 'default')).ok, true);
  let rejectJava;
  dependencies.inspectJava = () =>
    new Promise((_resolve, reject) => {
      rejectJava = reject;
    });
  const checking = dispatch('java:check', event, 'default');
  assert.match((await dispatch('java:check', event, 'default')).error, /уже выполняется/);
  rejectJava(new Error('PRIVATE-STDERR'));
  assert.equal((await checking).error.includes('PRIVATE'), false);
  dependencies.inspectJava = async () => ({
    usable: false,
    major: null,
    bits: null,
    message: 'Unavailable',
  });
  assert.equal((await dispatch('java:check', event, 'default')).ok, true);
});

test('application shutdown aborts an active Java check and waits for its settlement before releasing running', async (t) => {
  const { dispatch, event, dependencies, application } = await fixture(t);
  let receivedSignal;
  let release;
  dependencies.inspectJava = (_path, signal) => {
    receivedSignal = signal;
    return new Promise((resolve) => {
      release = resolve;
    });
  };
  const checking = dispatch('java:check', event, 'default');
  await new Promise((resolve) => setImmediate(resolve));
  let closed = false;
  let closing;
  try {
    assert.equal(application.running, true);
    closing = application.shutdown().then(() => {
      closed = true;
    });
    await new Promise((resolve) => setImmediate(resolve));
    assert.equal(receivedSignal?.aborted, true);
    assert.equal(closed, false);
    assert.equal(application.running, true);
  } finally {
    release({ usable: false, major: null, bits: null, message: 'Cancelled' });
    await checking;
    await closing;
  }
  assert.equal(closed, true);
  assert.equal(application.running, false);
  assert.match((await dispatch('java:check', event, 'default')).error, /завершает работу/);
});

test('folder/installation commands use repository-owned paths and known IDs; shutdown is delegated', async (t) => {
  const { dispatch, event, root, calls, application } = await fixture(t);
  assert.deepEqual(await dispatch('profile:open', event, 'default'), { ok: true, value: null });
  for (const channel of ['installation:get', 'installation:start', 'installation:cancel'])
    assert.equal((await dispatch(channel, event, 'default')).ok, true);
  await application.shutdown();
  assert.deepEqual(calls, [
    ['open', path.join(root, 'instances/default')],
    ['status', 'default'],
    ['start', 'default'],
    ['cancel', 'default'],
    ['shutdown'],
  ]);
});

test('IPC boundary refuses foreign events/prototype names and redacts unexpected failures', async (t) => {
  const { application, dispatch, event, profiles, createDispatcher } = await fixture(t);
  const { UserFacingError } = await import('../src/main/application/user-facing-error.ts');
  const before = profiles.snapshot();
  assert.deepEqual(await dispatch('profile:create', {}), {
    ok: false,
    error: 'Действие недоступно.',
  });
  assert.deepEqual(profiles.snapshot(), before);
  for (const name of ['constructor', 'toString', 'missing'])
    assert.deepEqual(await dispatch(name, event), { ok: false, error: 'Действие недоступно.' });
  for (const error of [new Error('PRIVATE-TOKEN'), { secret: 'PRIVATE-TOKEN' }, 'PRIVATE-TOKEN']) {
    const failing = createDispatcher(
      {
        ...application.commands,
        'state:get': () => {
          throw error;
        },
      },
      () => true,
    );
    const result = await failing('state:get', event);
    assert.equal(result.ok, false);
    assert.equal(JSON.stringify(result).includes('PRIVATE'), false);
  }
  const expected = createDispatcher(
    {
      ...application.commands,
      'state:get': () => {
        throw new UserFacingError('Понятное сообщение.');
      },
    },
    () => true,
  );
  assert.deepEqual(await expected('state:get', event), { ok: false, error: 'Понятное сообщение.' });
});

test('asset adapter serves only fixed local assets with unchanged CSP', async (t) => {
  const { root } = await fixture(t);
  const { createAssetHandler } = await import('../src/main/electron/assets.ts');
  const { CSP } = await import('../src/main/security.ts');
  const handle = createAssetHandler(root);
  await fs.writeFile(path.join(root, 'index.html'), 'own renderer');
  const response = await handle(new Request('space://app/index.html'));
  assert.equal(await response.text(), 'own renderer');
  assert.equal(response.headers.get('content-security-policy'), CSP);
  assert.equal(response.headers.get('x-content-type-options'), 'nosniff');
  const logoBytes = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);
  await fs.writeFile(path.join(root, 'logo.png'), logoBytes);
  const logo = await handle(new Request('space://app/logo.png'));
  assert.equal(logo.status, 200);
  assert.equal(logo.headers.get('content-type'), 'image/png');
  assert.equal(logo.headers.get('content-security-policy'), CSP);
  assert.deepEqual(Buffer.from(await logo.arrayBuffer()), logoBytes);
  for (const [url, method] of [
    ['space://app/main.cjs', 'GET'],
    ['space://app/index.html?extra=1', 'GET'],
    ['space://other/index.html', 'GET'],
    ['space://app/index.html', 'POST'],
    ['space://app/app.css', 'GET'],
  ]) {
    assert.equal((await handle(new Request(url, { method }))).status, 404);
  }
});

test('local launch accepts only known profile ID and main-saved nickname; installation and launch exclude each other', async (t) => {
  const { dispatch, event, dependencies, profiles, calls } = await fixture(t);
  await profiles.saveNickname('SpaceTester');
  assert.equal((await dispatch('launch:start', event, { id: 'default', args: ['bad'] })).ok, false);
  dependencies.installation.running = true;
  assert.equal((await dispatch('launch:start', event, 'default')).ok, false);
  assert.deepEqual(calls, []);
  dependencies.installation.running = false;
  assert.equal((await dispatch('launch:start', event, 'default')).ok, true);
  assert.deepEqual(calls, [['launch', 'default', 'SpaceTester']]);
  dependencies.launch.running = true;
  assert.equal((await dispatch('installation:start', event, 'default')).ok, false);
  assert.equal((await dispatch('local:save', event, { nickname: 'other', args: [] })).ok, false);
});
