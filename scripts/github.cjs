const { execFileSync } = require('node:child_process');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
function github(args, input) {
  const env = { ...process.env, GH_PROMPT_DISABLED: '1', GCM_INTERACTIVE: 'never' };
  if (!env.GH_TOKEN && !env.GITHUB_TOKEN) {
    let credential;
    try {
      credential = execFileSync('git', ['-c', 'credential.interactive=false', 'credential', 'fill'], {
        cwd: root, input: 'protocol=https\nhost=github.com\n\n', encoding: 'utf8', env, stdio: ['pipe', 'pipe', 'pipe']
      });
    } catch { throw new Error('GitHub credentials unavailable. Use gh auth login or configure Git Credential Manager.'); }
    const password = credential.split(/\r?\n/).find(line => line.startsWith('password='));
    if (!password) throw new Error('GitHub credential does not contain a token.');
    env.GH_TOKEN = password.slice('password='.length);
  }
  const binary = process.platform === 'win32' ? path.join(root, '.tools/gh/bin/gh.exe') : 'gh';
  return execFileSync(binary, args, { cwd: root, env, input, encoding: 'utf8', stdio: ['pipe', 'pipe', 'pipe'] });
}
module.exports = { github };
if (require.main === module) {
  try { process.stdout.write(github(process.argv.slice(2))); }
  catch { console.error('GitHub command failed. Check authentication, arguments and repository permissions.'); process.exitCode = 1; }
}
