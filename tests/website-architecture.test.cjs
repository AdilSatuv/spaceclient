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
  'apps/website/src',
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
  if (!specifier.startsWith('.') && !specifier.startsWith('@/')) return null;
  const base = specifier.startsWith('@/')
    ? path.join(root, 'apps/website/src', specifier.slice(2))
    : path.resolve(path.dirname(source.file), specifier);
  const file = [base, base + '.ts', base + '.tsx', path.join(base, 'index.ts')].find(
    (candidate) => fs.existsSync(candidate) && fs.statSync(candidate).isFile(),
  );
  assert.ok(file, `${source.name}: unresolved ${specifier}`);
  return path.relative(root, file).replaceAll('\\', '/');
}

const fsdRoot = 'apps/website/src/';
const layers = ['_app', '_pages', 'features', 'entities', 'shared'];
function owner(name) {
  if (!name.startsWith(fsdRoot)) return null;
  const [layer, slice, ...rest] = name.slice(fsdRoot.length).split('/');
  assert.ok(layers.includes(layer), `${name}: unknown FSD layer`);
  const sliced = !['_app', 'shared'].includes(layer);
  return {
    layer,
    rank: layers.indexOf(layer),
    key: sliced ? `${layer}/${slice}` : layer,
    sliced,
    rest,
    slice,
  };
}
function checkFsdImport(from, to) {
  const source = owner(from);
  const destination = owner(to);
  assert.ok(
    !source || !to.startsWith('apps/website/app/'),
    `${from}: FSD code cannot depend on Next route adapters`,
  );
  if (!destination) return;
  const message = `${from} -> ${to}`;
  if (source && source.key !== destination.key) {
    assert.ok(destination.rank > source.rank, `Upward or cross-slice dependency: ${message}`);
  }
  if (source?.key === destination.key) return;
  if (destination.sliced) {
    assert.ok(
      destination.rest.length === 1 && /^index(?:\.server)?\.ts$/.test(destination.rest[0]),
      `Bypassed slice public API: ${message}`,
    );
  }
  if (destination.layer === 'shared' && destination.slice === 'api') {
    assert.ok(
      destination.rest.length === 1 && /^index(?:\.server)?\.ts$/.test(destination.rest[0]),
      `Bypassed shared API entry point: ${message}`,
    );
  }
}

test('FSD imports go down through public APIs and slices stay isolated', () => {
  for (const source of sources.filter((item) => item.name.startsWith('apps/website/'))) {
    owner(source.name);
    for (const item of imports(source.ast)) {
      const local = target(source, item.specifier);
      if (local) checkFsdImport(source.name, local);
    }
  }
});

test('FSD guard rejects upward, sibling-slice and private imports', () => {
  const page = `${fsdRoot}_pages/download/ui/page.tsx`;
  const release = `${fsdRoot}entities/release/index.ts`;
  assert.doesNotThrow(() => checkFsdImport(page, release));
  assert.doesNotThrow(() => checkFsdImport(page, `${fsdRoot}_pages/download/model/query.ts`));
  for (const [from, to] of [
    [release, page],
    [release, 'apps/website/app/page.tsx'],
    [page, `${fsdRoot}_pages/help/index.server.ts`],
    [page, `${fsdRoot}entities/release/model/recommend-release.ts`],
    [page, `${fsdRoot}shared/api/server.ts`],
    [`${fsdRoot}features/motion-preference/index.ts`, `${fsdRoot}features/account/index.ts`],
  ])
    assert.throws(() => checkFsdImport(from, to));
});

test('Next route files remain thin adapters with statically declared route configuration', () => {
  for (const source of sources.filter((item) => item.name.startsWith('apps/website/app/'))) {
    for (const statement of source.ast.statements) {
      if (ts.isImportDeclaration(statement) || ts.isExportDeclaration(statement)) continue;
      if (
        ts.isExpressionStatement(statement) &&
        ts.isStringLiteral(statement.expression) &&
        statement.expression.text === 'use client'
      )
        continue;
      assert.ok(
        ts.isVariableStatement(statement),
        `${source.name}: route contains presentation or business logic`,
      );
      for (const declaration of statement.declarationList.declarations) {
        assert.ok(
          ts.isIdentifier(declaration.name) &&
            ['dynamic', 'dynamicParams', 'revalidate', 'runtime', 'preferredRegion'].includes(
              declaration.name.text,
            ),
          `${source.name}: unexpected route declaration`,
        );
        assert.ok(
          declaration.initializer &&
            [
              ts.SyntaxKind.StringLiteral,
              ts.SyntaxKind.NumericLiteral,
              ts.SyntaxKind.TrueKeyword,
              ts.SyntaxKind.FalseKeyword,
            ].includes(declaration.initializer.kind),
          `${source.name}: route configuration must stay static`,
        );
      }
    }
  }
});

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
    }
    // App Router files compose services and views; raw fetch belongs to the API adapter.
    if (source.name.startsWith('apps/website/')) {
      function visit(node) {
        if (
          ts.isCallExpression(node) &&
          ts.isIdentifier(node.expression) &&
          node.expression.text === 'fetch'
        ) {
          assert.ok(
            source.name === 'apps/website/src/shared/api/content-client.ts',
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
  const entry = graph.get('apps/website/src/shared/api/index.server.ts');
  assert.ok(imports(entry.ast).some((item) => item.specifier === 'server-only'));
  for (const source of sources.filter((item) => item.name.endsWith('/index.server.ts'))) {
    assert.ok(
      imports(source.ast).some((item) => item.specifier === 'server-only'),
      `${source.name}: server public API must be marked`,
    );
  }
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
      !source.name.startsWith('apps/website/src/shared/api/'),
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
