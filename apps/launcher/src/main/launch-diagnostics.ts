type Diagnostic = {
  patterns: readonly string[];
  label: string;
  failure?: { priority: number; message: string };
};

const diagnostics: readonly Diagnostic[] = [
  { patterns: ['Loading Minecraft'], label: 'Загрузчик начал запуск Minecraft.' },
  {
    patterns: ['Space Client 0.1.0 initialized'],
    label: 'Игровой мод Space инициализирован.',
  },
  { patterns: ['OpenAL initialized'], label: 'Звуковой движок инициализирован.' },
  {
    patterns: ['OutOfMemoryError', 'Could not reserve enough space'],
    label: 'Java сообщила о нехватке памяти.',
    failure: {
      priority: 50,
      message:
        'Java сообщила о нехватке памяти. Проверьте доступную память и настройку RAM профиля; закройте лишние приложения и повторите запуск.',
    },
  },
  {
    patterns: ['Failed to create display', 'Pixel format not accelerated'],
    label: 'Не удалось создать игровое окно OpenGL.',
    failure: {
      priority: 40,
      message:
        'Не удалось создать игровое окно OpenGL. Проверьте поддержку OpenGL и обновите драйвер видеокарты, затем повторите запуск.',
    },
  },
  {
    patterns: ['UnsupportedClassVersionError'],
    label: 'Java сообщила о несовместимой версии классов.',
    failure: {
      priority: 30,
      message:
        'Java сообщила о несовместимой версии классов. Выберите Java 21 x64 в профиле и выполните «Проверить и восстановить».',
    },
  },
  {
    patterns: ['Could not find or load main class', 'NoClassDefFoundError'],
    label: 'Java не смогла загрузить необходимый класс.',
    failure: {
      priority: 20,
      message:
        'Java не смогла загрузить необходимый класс. Выполните «Проверить и восстановить» и повторите запуск.',
    },
  },
  {
    patterns: ['Exception in thread'],
    label: 'Java сообщила об ошибке игрового потока.',
    failure: {
      priority: 10,
      message:
        'Java сообщила об ошибке игрового потока. Проверьте Java в профиле и выполните «Проверить и восстановить»; если ошибка повторится, сообщите код выхода.',
    },
  },
];
const tailLength =
  Math.max(...diagnostics.flatMap((item) => item.patterns.map((p) => p.length))) - 1;

/** Only fixed labels escape this main-only collector; raw tails are bounded per stream. */
export class LaunchDiagnostics {
  private tails = { stdout: '', stderr: '' };
  private seen = new Set<Diagnostic>();
  private failure: Diagnostic['failure'];

  accept(stream: 'stdout' | 'stderr', chunk: Buffer): string[] {
    const labels: string[] = [];
    // Scan the whole chunk in bounded blocks, including signatures across block/data boundaries.
    for (let offset = 0; offset < chunk.length; offset += 8192) {
      const text =
        this.tails[stream] + chunk.toString('utf8', offset, Math.min(offset + 8192, chunk.length));
      for (const item of diagnostics) {
        if (!this.seen.has(item) && item.patterns.some((pattern) => text.includes(pattern))) {
          this.seen.add(item);
          labels.push(item.label);
          if (item.failure && (!this.failure || item.failure.priority > this.failure.priority))
            this.failure = item.failure;
        }
      }
      this.tails[stream] = text.slice(-tailLength);
    }
    return labels;
  }

  finish(): string | undefined {
    this.tails = { stdout: '', stderr: '' };
    return this.failure?.message;
  }
}
