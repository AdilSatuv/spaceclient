const { test } = require('node:test');
const assert = require('node:assert/strict');

test('diagnostics recognize supported failures across every data boundary and emit fixed labels once', async () => {
  const { LaunchDiagnostics } = await import('../src/main/launch-diagnostics.ts');
  for (const [signature, cause, action] of [
    ['OutOfMemoryError', /нехватке памяти/, /RAM/],
    ['Could not reserve enough space', /нехватке памяти/, /RAM/],
    ['Failed to create display', /OpenGL/, /драйвер/],
    ['Pixel format not accelerated', /OpenGL/, /драйвер/],
    ['UnsupportedClassVersionError', /версии классов/, /Java 21 x64/],
    ['Could not find or load main class', /необходимый класс/, /Проверить и восстановить/],
    ['NoClassDefFoundError', /необходимый класс/, /Проверить и восстановить/],
    ['Exception in thread', /ошибке игрового потока/, /код выхода/],
  ]) {
    for (const stream of ['stdout', 'stderr']) {
      for (let split = 1; split < signature.length; split++) {
        const collector = new LaunchDiagnostics();
        const labels = [
          ...collector.accept(
            stream,
            Buffer.from('SECRET-token C:\\private\\ ' + signature.slice(0, split)),
          ),
          ...collector.accept(stream, Buffer.from(signature.slice(split) + ' SECRET-nickname')),
        ];
        assert.equal(labels.length, 1, `${stream}: ${signature}, split ${split}`);
        assert.deepEqual(collector.accept(stream, Buffer.from(signature)), []);
        const message = collector.finish();
        assert.match(message, cause);
        assert.match(message, action);
        assert.doesNotMatch(JSON.stringify({ labels, message }), /SECRET|private/);
      }
    }
  }
});

test('stdout and stderr cannot fabricate a failure by joining unrelated partial lines', async () => {
  const { LaunchDiagnostics } = await import('../src/main/launch-diagnostics.ts');
  const collector = new LaunchDiagnostics();
  assert.deepEqual(collector.accept('stdout', Buffer.from('OutOfMem')), []);
  assert.deepEqual(collector.accept('stderr', Buffer.from('oryError')), []);
  assert.equal(collector.finish(), undefined);
});

test('bounded processing finds signatures at internal block boundaries and after a long prefix', async () => {
  const { LaunchDiagnostics } = await import('../src/main/launch-diagnostics.ts');
  for (const prefix of [8191, 8192, 16380, 65536]) {
    const collector = new LaunchDiagnostics();
    assert.deepEqual(
      collector.accept('stderr', Buffer.from('x'.repeat(prefix) + 'OutOfMemoryError')),
      ['Java сообщила о нехватке памяти.'],
    );
    assert.match(collector.finish(), /RAM/);
  }
});

test('a specific failure wins over a generic exception regardless of stream order', async () => {
  const { LaunchDiagnostics } = await import('../src/main/launch-diagnostics.ts');
  for (const signatures of [
    ['Exception in thread', 'NoClassDefFoundError', 'OutOfMemoryError'],
    ['OutOfMemoryError', 'NoClassDefFoundError', 'Exception in thread'],
  ]) {
    const collector = new LaunchDiagnostics();
    for (let i = 0; i < signatures.length; i++)
      collector.accept(i % 2 ? 'stdout' : 'stderr', Buffer.from(signatures[i]));
    assert.match(collector.finish(), /нехватке памяти/);
  }
});

test('success milestones stay fixed and unknown output does not claim a failure cause', async () => {
  const { LaunchDiagnostics } = await import('../src/main/launch-diagnostics.ts');
  const collector = new LaunchDiagnostics();
  assert.deepEqual(
    collector.accept(
      'stdout',
      Buffer.from('Loading Minecraft\nSpace Client 0.1.0 initialized\nOpenAL initialized'),
    ),
    [
      'Загрузчик начал запуск Minecraft.',
      'Игровой мод Space инициализирован.',
      'Звуковой движок инициализирован.',
    ],
  );
  assert.deepEqual(collector.accept('stderr', Buffer.from('SECRET --accessToken SECRET')), []);
  assert.equal(collector.finish(), undefined);
});
