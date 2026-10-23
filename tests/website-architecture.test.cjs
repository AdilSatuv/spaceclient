const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const ts = require('typescript');
const root = path.resolve(__dirname, '..');
function files(directory) {
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const file = path.join(directory, entry.name);
    return entry.isDirectory() ? files(file) : /\.tsx?$/.test(entry.name) ? [file] : [];
  });
}
const sources = [
  'packages/content-contracts/src',
  'apps/api/src',
  'apps/website/app',
  'apps/website/components',
  'apps/website/lib',
]
  .flatMap((directory) => files(path.join(root, directory)))
  .map((file) => ({
    file,
    name: path.relative(root, file).replaceAll('\\', '/'),
    ast: ts.createSourceFile(file, fs.readFileSync(file, 'utf8'), ts.ScriptTarget.Latest, true),
  }));
function imports(ast) {
  const result = [];
  function visit(node) {
    if ((ts.isImportDeclaration(node) || ts.isExportDeclaration(node)) && node.moduleSpecifier) {
      const clause = node.importClause;
      const bindings = clause?.namedBindings;
      const exports = node.exportClause;
      const typeOnly =
        !!node.isTypeOnly ||
        !!clause?.isTypeOnly ||
        (bindings &&
          ts.isNamedImports(bindings) &&
          !clause.name &&
          bindings.elements.length > 0 &&
          bindings.elements.every((item) => item.isTypeOnly)) ||
        (exports &&
          ts.isNamedExports(exports) &&
          exports.elements.length > 0 &&
          exports.elements.every((item) => item.isTypeOnly));
      result.push({ specifier: node.moduleSpecifier.text, typeOnly });
    }
    if (
      ts.isCallExpression(node) &&
      (node.expression.kind === ts.SyntaxKind.ImportKeyword ||
        (ts.isIdentifier(node.expression) && node.expression.text === 'require'))
    ) {
      assert.ok(
        node.arguments[0] && ts.isStringLiteral(node.arguments[0]),
        'Use statically resolvable dependencies',
      );
      result.push({ specifier: node.arguments[0].text, typeOnly: false });
    }
    ts.forEachChild(node, visit);
  }
  visit(ast);
  return result;
}
function target(source, specifier) {
  if (specifier === '@space/content-contracts') return 'packages/content-contracts/src/index.ts';
  if (!specifier.startsWith('.')) return null;
  const base = path.resolve(path.dirname(source.file), specifier);
  const file = [base, base + '.ts', base + '.tsx', path.join(base, 'index.ts')].find(
    (candidate) => fs.existsSync(candidate) && fs.statSync(candidate).isFile(),
  );
  assert.ok(file, `${source.name}: unresolved ${specifier}`);
  return path.relative(root, file).replaceAll('\\', '/');
}

test('web layers depend on shared contracts without importing another application', () => {
  for (const source of sources) {
    for (const dependency of imports(source.ast)) {
      const local = target(source, dependency.specifier);
      const message = `${source.name} -> ${dependency.specifier}`;
      if (source.name.startsWith('packages/'))
        assert.ok(local?.startsWith('packages/content-contracts/src/'), message);
      if (source.name.startsWith('apps/api/'))
        assert.ok(
          !local || local.startsWith('apps/api/') || local.startsWith('packages/'),
          message,
        );
      if (source.name.startsWith('apps/api/src/content/application/')) {
        assert.ok(
          local?.startsWith('apps/api/src/content/application/') || local?.startsWith('packages/'),
          message,
        );
      }
      if (source.name.startsWith('apps/api/src/content/http/')) {
        assert.ok(
          !local ||
            local.startsWith('apps/api/src/content/http/') ||
            local.startsWith('apps/api/src/content/application/') ||
            local.startsWith('packages/'),
          message,
        );
      }
      if (source.name.startsWith('apps/website/')) {
        assert.ok(
          !local || local.startsWith('apps/website/') || local.startsWith('packages/'),
          message,
        );
      }
      if (source.name.startsWith('apps/website/lib/server/')) {
        assert.ok(
          !local || local.startsWith('apps/website/lib/') || local.startsWith('packages/'),
          message,
        );
      }
    }
    // App Router files compose services and views; API transport belongs to lib/server.
    if (source.name.startsWith('apps/website/')) {
      function visit(node) {
        if (
          ts.isCallExpression(node) &&
          ts.isIdentifier(node.expression) &&
          node.expression.text === 'fetch'
        ) {
          assert.ok(
            source.name.startsWith('apps/website/lib/server/'),
            `${source.name}: raw fetch outside server adapter`,
          );
        }
        ts.forEachChild(node, visit);
      }
      visit(source.ast);
    }
  }
});

test('client entry points cannot transitively import server adapters or private configuration', () => {
  const graph = new Map(sources.map((source) => [source.name, source]));
  const entry = graph.get('apps/website/lib/server/api.ts');
  assert.ok(imports(entry.ast).some((item) => item.specifier === 'server-only'));
  const clients = sources.filter((source) =>
    source.ast.statements.some(
      (node) =>
        ts.isExpressionStatement(node) &&
        ts.isStringLiteral(node.expression) &&
        node.expression.text === 'use client',
    ),
  );
  assert.ok(clients.length > 0);
  function visit(source, seen = new Set()) {
    if (seen.has(source.name)) return;
    seen.add(source.name);
    assert.ok(
      !source.name.startsWith('apps/website/lib/server/'),
      `${source.name}: server module in client graph`,
    );
    assert.doesNotMatch(source.ast.text, /process\.env\.SPACE_API_URL/);
    for (const item of imports(source.ast).filter((item) => !item.typeOnly)) {
      assert.notEqual(item.specifier, 'server-only');
      const next = graph.get(target(source, item.specifier));
      if (next) visit(next, seen);
    }
  }
  for (const source of clients) visit(source);
});

test('web source runtime graph has no circular imports', () => {
  const graph = new Map(
    sources.map((source) => [
      source.name,
      imports(source.ast)
        .filter((item) => !item.typeOnly)
        .map((item) => target(source, item.specifier))
        .filter(Boolean),
    ]),
  );
  const done = new Set();
  function visit(name, active = []) {
    assert.ok(!active.includes(name), `Runtime cycle: ${[...active, name].join(' -> ')}`);
    if (done.has(name)) return;
    for (const next of graph.get(name) ?? []) visit(next, [...active, name]);
    done.add(name);
  }
  for (const name of graph.keys()) visit(name);
});
