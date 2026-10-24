import { spawn } from 'node:child_process';
import { createInterface } from 'node:readline';
import { fileURLToPath } from 'node:url';

// Importable by node_repl. All timing belongs to the Node 24 collector, including the
// begin/end IPC latency, so screenshots cannot acquire artificially precise timestamps.
export async function connectStartupObserver(sourceRoot) {
  const script = fileURLToPath(new URL('./startup.cjs', import.meta.url));
  const child = spawn('node', [script, sourceRoot], {
    windowsHide: true,
    shell: false,
    stdio: ['pipe', 'pipe', 'pipe'],
  });
  let sequence = 0;
  let connected = true;
  const pending = new Map();
  let readyResolve, readyReject;
  const ready = new Promise((resolve, reject) => {
    readyResolve = resolve;
    readyReject = reject;
  });
  let stderr = '';
  child.stderr.on('data', (chunk) => {
    stderr = (stderr + chunk).slice(-4096);
  });
  const lines = createInterface({ input: child.stdout, crlfDelay: Infinity });
  lines.on('line', (line) => {
    const message = JSON.parse(line);
    if (message.event === 'ready') readyResolve(message);
    else {
      const handler = pending.get(message.id);
      pending.delete(message.id);
      if (message.error) handler?.reject(new Error(message.error));
      else handler?.resolve(message.result);
    }
  });
  const failed = () => {
    connected = false;
    const error = new Error('Startup collector stopped: ' + stderr);
    readyReject(error);
    for (const handler of pending.values()) handler.reject(error);
    pending.clear();
  };
  child.once('error', failed);
  child.once('exit', failed);
  child.stdin.on('error', failed);
  const initialized = await ready;
  return {
    directory: initialized.directory,
    request(command, input = {}) {
      if (!connected) return Promise.reject(new Error('Startup observer is disconnected'));
      const id = ++sequence;
      return new Promise((resolve, reject) => {
        pending.set(id, { resolve, reject });
        child.stdin.write(JSON.stringify({ ...input, id, command }) + '\n');
      });
    },
    disconnect() {
      connected = false;
      child.stdin.end();
    },
  };
}
