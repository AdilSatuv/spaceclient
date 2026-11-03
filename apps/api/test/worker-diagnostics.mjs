// Test-only observations. No uncaughtException/unhandledRejection handlers: failure semantics stay intact.
import { appendFileSync, mkdirSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, resolve, join } from 'node:path';
import { randomUUID } from 'node:crypto';
import { threadId, isMainThread } from 'node:worker_threads';
const directory = resolve(dirname(fileURLToPath(import.meta.url)), '../../../.reports/api-workers');
const output = join(directory, `${process.pid}-${threadId}-${randomUUID()}.jsonl`);
function record(event, extra = {}) {
  try {
    mkdirSync(directory, { recursive: true });
    appendFileSync(
      output,
      JSON.stringify({
        event,
        at: new Date().toISOString(),
        pid: process.pid,
        ppid: process.ppid,
        threadId,
        isMainThread,
        node: process.version,
        testWorker: !!process.env.NODE_TEST_CONTEXT,
        memory: process.memoryUsage(),
        resources: process.getActiveResourcesInfo(),
        ...extra,
      }) + '\n',
    );
  } catch {
    process.stderr.write('API test worker diagnostics unavailable.\n');
  }
}
record('start');
process.on('uncaughtExceptionMonitor', (error, origin) =>
  record('uncaughtExceptionMonitor', {
    origin,
    name: error?.name,
    code: /^[A-Z0-9_]+$/.test(error?.code ?? '') ? error.code : undefined,
    frames: String(error?.stack ?? '')
      .split('\n')
      .slice(1, 12)
      .filter((line) => /^\s+at /.test(line)),
  }),
);
process.on('exit', (code) => record('exit', { code }));
