const { test } = require('node:test');
const assert = require('node:assert/strict');
const { EventEmitter } = require('node:events');
const { execFile } = require('node:child_process');
const selected = 'C:/private-runtime/bin/java.exe';
const properties = 'java.specification.version = 21\nsun.arch.data.model = 64';
const tick = () => new Promise((resolve) => setImmediate(resolve));

test('an already aborted Java probe does not touch the disk or start a process', async () => {
  const { inspectJava } = await import('../src/main/java.ts');
  const controller = new AbortController();
  controller.abort(new Error('SECRET-reason'));
  const calls = [];
  await assert.rejects(
    inspectJava(selected, controller.signal, {
      stat: async () => {
        calls.push('stat');
      },
      execFile: () => {
        calls.push('execFile');
      },
    }),
    (error) => /отменена/.test(error.message) && !error.message.includes('SECRET'),
  );
  assert.deepEqual(calls, []);
});

test('cancellation during Java path lookup prevents spawn even when lookup later succeeds', async () => {
  const { inspectJava } = await import('../src/main/java.ts');
  const controller = new AbortController();
  let release;
  let calls = 0;
  const probe = inspectJava(selected, controller.signal, {
    stat: () =>
      new Promise((resolve) => {
        release = resolve;
      }),
    execFile: () => {
      calls++;
    },
  });
  const rejected = assert.rejects(probe, /отменена/);
  controller.abort();
  release?.({ isFile: () => true });
  await rejected;
  assert.equal(calls, 0);
});

test('abort callback does not release the probe until close, and fixed execution limits are preserved', async () => {
  const { inspectJava } = await import('../src/main/java.ts');
  const controller = new AbortController();
  const child = new EventEmitter();
  let callback;
  let options;
  const probe = inspectJava(selected, controller.signal, {
    stat: async () => ({ isFile: () => true }),
    execFile: (executable, args, input, complete) => {
      assert.equal(executable, selected);
      assert.deepEqual(args, ['-XshowSettings:properties', '-version']);
      options = input;
      callback = complete;
      return child;
    },
  });
  let settled = false;
  const observing = probe.then(
    () => {
      settled = true;
    },
    () => {
      settled = true;
    },
  );
  await tick();
  assert.equal(options.signal, controller.signal);
  assert.equal(options.timeout, 5000);
  assert.equal(options.maxBuffer, 65536);
  assert.equal(options.shell, false);
  assert.equal(options.windowsHide, true);
  for (const key of ['JAVA_TOOL_OPTIONS', '_JAVA_OPTIONS', 'JDK_JAVA_OPTIONS', 'CLASSPATH'])
    assert.equal(options.env[key], undefined);
  controller.abort(new Error('SECRET-abort-reason'));
  callback(new Error('SECRET-executable stderr command'), 'SECRET-stdout', 'SECRET-stderr');
  await tick();
  assert.equal(settled, false);
  child.emit('close', null, 'SIGTERM');
  await assert.rejects(probe, (error) => error.message === 'Проверка Java отменена.');
  await observing;
  assert.equal(settled, true);
});

test('normal probe output is parsed only after close and failures never expose OS output', async () => {
  const { inspectJava } = await import('../src/main/java.ts');
  for (const failed of [false, true]) {
    const child = new EventEmitter();
    let callback;
    const probe = inspectJava(selected, undefined, {
      stat: async () => ({ isFile: () => true }),
      execFile: (_executable, _args, _options, complete) => {
        callback = complete;
        return child;
      },
    });
    let settled = false;
    const observing = probe.then(
      () => {
        settled = true;
      },
      () => {
        settled = true;
      },
    );
    await tick();
    callback(
      failed ? new Error('SECRET-path') : null,
      'SECRET-stdout',
      failed ? 'SECRET-stderr' : properties,
    );
    await tick();
    assert.equal(settled, false);
    child.emit('close', failed ? 1 : 0, null);
    if (failed)
      await assert.rejects(
        probe,
        (error) => /не ответила/.test(error.message) && !error.message.includes('SECRET'),
      );
    else
      assert.deepEqual(await probe, {
        usable: true,
        major: 21,
        bits: 64,
        message: 'Java 21 · 64 бита. Проверка пройдена.',
      });
    await observing;
  }
  await assert.rejects(
    inspectJava(selected, undefined, {
      stat: async () => ({ isFile: () => true }),
      execFile: () => {
        throw new Error('SECRET-spawn-path');
      },
    }),
    (error) => /не ответила/.test(error.message) && !error.message.includes('SECRET'),
  );
});

test('cancellation wins over a failed path lookup without exposing the abort reason', async () => {
  const { inspectJava } = await import('../src/main/java.ts');
  const controller = new AbortController();
  let fail;
  const probe = inspectJava(selected, controller.signal, {
    stat: () =>
      new Promise((_resolve, reject) => {
        fail = reject;
      }),
    execFile: () => {
      assert.fail('a cancelled probe must not spawn');
    },
  });
  controller.abort(new Error('SECRET-reason'));
  fail(new Error('SECRET-stat-path'));
  await assert.rejects(probe, (error) => error.message === 'Проверка Java отменена.');
});

test(
  'native execFile cancellation closes only its owned hidden helper before the probe settles',
  { timeout: 10000 },
  async () => {
    const { inspectJava } = await import('../src/main/java.ts');
    const controller = new AbortController();
    const bystander = execFile(
      process.execPath,
      ['-e', "process.stdout.write('other-ready'); setInterval(() => {}, 1000)"],
      { windowsHide: true, timeout: 5000, maxBuffer: 65536 },
      () => {},
    );
    const otherReady = new Promise((resolve, reject) => {
      bystander.stdout.once('data', resolve);
      bystander.once('error', reject);
      bystander.once('close', () => reject(new Error('Other helper exited before readiness')));
    });
    const otherClosed = new Promise((resolve) => bystander.once('close', resolve));
    let child;
    let ready;
    let closed;
    let didClose = false;
    const began = new Promise((resolve) => {
      ready = resolve;
    });
    const stopped = new Promise((resolve) => {
      closed = resolve;
    });
    const probe = inspectJava(selected, controller.signal, {
      stat: async () => ({ isFile: () => true }),
      // Test-only transport: a real owned Node helper stands in for a deliberately slow java -version.
      execFile: (_executable, _args, options, callback) => {
        child = execFile(
          process.execPath,
          ['-e', "process.stdout.write('probe-ready'); setInterval(() => {}, 1000)"],
          options,
          callback,
        );
        child.stdout.once('data', ready);
        child.once('close', () => {
          didClose = true;
          closed();
        });
        return child;
      },
    });
    probe.catch(() => {});
    try {
      await Promise.race([
        Promise.all([began, otherReady]),
        probe.then(() => {
          throw new Error('Helper exited before readiness');
        }),
      ]);
      assert.equal(didClose, false);
      controller.abort(new Error('SECRET-native-reason'));
      await assert.rejects(probe, (error) => error.message === 'Проверка Java отменена.');
      assert.equal(didClose, true);
      assert.equal(child.killed, true);
      assert.equal(bystander.killed, false);
      assert.equal(bystander.exitCode, null);
      assert.equal(bystander.signalCode, null);
      await stopped;
    } finally {
      if (child && !didClose) {
        child.kill();
        await stopped;
      }
      if (bystander.exitCode === null && bystander.signalCode === null) bystander.kill();
      await otherClosed;
      await probe.catch(() => {});
    }
  },
);
