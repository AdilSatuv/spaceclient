const { test } = require('node:test');
const assert = require('node:assert/strict');
const tasks = require('../docs/tasks.json');
test('task dependencies exist, are acyclic and have acceptance criteria', () => {
  const indexed = new Map(tasks.map(task => [task.id, task]));
  assert.equal(indexed.size, tasks.length);
  const visited = new Set();
  function visit(id, stack = new Set()) {
    assert.ok(indexed.has(id), `Missing dependency ${id}`);
    assert.ok(!stack.has(id), `Dependency cycle at ${id}`);
    if (visited.has(id)) return;
    const task = indexed.get(id);
    assert.ok(task.acceptance.length >= 3);
    const branch = new Set([...stack, id]);
    task.depends.forEach(dependency => visit(dependency, branch));
    visited.add(id);
  }
  tasks.forEach(task => visit(task.id));
});
