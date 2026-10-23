const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const ts = require('typescript');
const root = path.resolve(__dirname, '../src');
function files(directory) {
  return fs.readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const file = path.join(directory, entry.name);
    return entry.isDirectory() ? files(file) : /\.tsx?$/.test(entry.name) ? [file] : [];
  });
}
const sources = files(root).map((file) => ({
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
        (exports &&
          ts.isNamedExports(exports) &&
          exports.elements.length > 0 &&
          exports.elements.every((item) => item.isTypeOnly)) ||
        (bindings &&
          ts.isNamedImports(bindings) &&
          !clause.name &&
          bindings.elements.length > 0 &&
          bindings.elements.every((item) => item.isTypeOnly));
      result.push({ specifier: node.moduleSpecifier.text, typeOnly });
    }
    if (
      ts.isCallExpression(node) &&
      (node.expression.kind === ts.SyntaxKind.ImportKeyword ||
        (ts.isIdentifier(node.expression) && node.expression.text === 'require')) &&
      node.arguments[0] &&
      ts.isStringLiteral(node.arguments[0])
    ) {
      result.push({ specifier: node.arguments[0].text, typeOnly: false });
    }
    ts.forEachChild(node, visit);
  }
  visit(ast);
  return result;
}
function target(source, specifier) {
  const base = specifier.startsWith('@/')
    ? path.join(root, specifier.slice(2))
    : specifier.startsWith('.')
      ? path.resolve(path.dirname(source.file), specifier)
      : null;
  if (!base) return null;
  const resolved = [
    base,
    base + '.ts',
    base + '.tsx',
    path.join(base, 'index.ts'),
    path.join(base, 'index.tsx'),
  ].find((candidate) => fs.existsSync(candidate) && fs.statSync(candidate).isFile());
  assert.ok(resolved, `${source.name}: unresolved ${specifier}`);
  return path.relative(root, resolved).replaceAll('\\', '/');
}

test('launcher dependency boundaries keep application and presentation models independent', () => {
  for (const source of sources) {
    for (const dependency of imports(source.ast)) {
      const local = target(source, dependency.specifier);
      const message = `${source.name} -> ${dependency.specifier}`;
      if (source.name.startsWith('shared/')) assert.ok(local?.startsWith('shared/'), message);
      if (source.name.startsWith('main/application/')) {
        assert.ok(local?.startsWith('main/application/') || local?.startsWith('shared/'), message);
      }
      if (source.name.startsWith('main/')) {
        assert.ok(!local || local.startsWith('main/') || local.startsWith('shared/'), message);
        if (dependency.specifier === 'electron') {
          assert.ok(
            source.name === 'main/index.ts' || source.name.startsWith('main/electron/'),
            message,
          );
        }
      }
      if (source.name.startsWith('renderer/')) {
        assert.ok(
          local
            ? local.startsWith('renderer/') || local.startsWith('shared/')
            : [
                'react',
                'react-dom/client',
                'react/jsx-runtime',
                'lucide-react',
                'class-variance-authority',
                'clsx',
                'tailwind-merge',
                '@radix-ui/react-slot',
              ].includes(dependency.specifier),
          message,
        );
      }
      if (source.name.startsWith('renderer/model/')) {
        assert.ok(local?.startsWith('renderer/model/') || local?.startsWith('shared/'), message);
      }
      if (source.name === 'preload.ts')
        assert.ok(dependency.specifier === 'electron' || local?.startsWith('shared/'), message);
    }
    if (source.name.startsWith('renderer/')) {
      function visit(node) {
        if (
          ts.isPropertyAccessExpression(node) &&
          node.name.text === 'space' &&
          ts.isIdentifier(node.expression) &&
          node.expression.text === 'window'
        ) {
          assert.equal(
            source.name,
            'renderer/index.tsx',
            'Only the renderer entry point may read the bridge',
          );
        }
        if (source.name.startsWith('renderer/model/') && ts.isIdentifier(node)) {
          assert.ok(
            !['window', 'document', 'ipcRenderer', 'process', 'require'].includes(node.text),
            `${source.name}: platform global ${node.text}`,
          );
        }
        ts.forEachChild(node, visit);
      }
      visit(source.ast);
    }
  }
});

test('local runtime dependencies contain no circular imports', () => {
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
