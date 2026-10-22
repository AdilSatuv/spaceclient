// Creates missing tasks only. Existing issue bodies, states and labels are user-owned.
const { github } = require('./github.cjs');
const tasks = require('../docs/tasks.json');
const fs = require('node:fs');
const path = require('node:path');
const repo = 'repos/AdilSatuv/spaceclient';
function api(endpoint, method = 'GET', data) {
  const result = JSON.parse(github(['api', `${repo}/${endpoint}`, '--method', method, ...(method === 'GET' ? ['--paginate', '--slurp'] : []), ...(data ? ['--input', '-'] : [])], data ? JSON.stringify(data) : undefined));
  return method === 'GET' ? result.flat() : result;
}
try {
  const issues = api('issues?state=all&per_page=100');
  const milestones = api('milestones?state=all&per_page=100');
  const links = new Map();
  for (const task of tasks) {
    let milestone = milestones.find(item => item.title === task.phase);
    if (!milestone) { milestone = api('milestones', 'POST', { title: task.phase }); milestones.push(milestone); }
    const marker = `<!-- space-task:${task.id} -->`;
    let issue = issues.find(item => !item.pull_request && item.body?.includes(marker));
    if (!issue) {
      const body = `${marker}\nПриоритет: ${task.priority}\n\nЗависимости: ${task.depends.map(id => links.get(id) || id).join(', ') || 'нет'}\n\n## Критерии готовности\n\n${task.acceptance.map(text => `- [ ] ${text}`).join('\n')}\n\nПлан: docs/IMPLEMENTATION-PLAN.md. Статус закрывается только после проверки критериев.`;
      issue = api('issues', 'POST', { title: `${task.id}: ${task.title}`, body, milestone: milestone.number });
      issues.push(issue);
    }
    links.set(task.id, issue.html_url);
    console.log(`${task.id} ${issue.html_url}`);
  }
  const text = '# Задачи Space Client\n\nЖивой статус — в GitHub Issues. Этот индекс содержит ссылки, а не копию статусов.\n\n' + tasks.map(task => `- [${task.id}: ${task.title}](${links.get(task.id)}) — ${task.priority}, ${task.phase}`).join('\n') + '\n';
  fs.writeFileSync(path.resolve(__dirname, '../docs/TASKS.md'), text);
} catch { console.error('Task sync stopped. Inspect GitHub Issues before retrying; rerun reuses task markers.'); process.exitCode = 1; }
