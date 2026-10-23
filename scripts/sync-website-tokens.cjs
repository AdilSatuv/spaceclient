const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const foundation = require('../docs/design/space-dark.tokens.json');
const controls = require('../docs/design/control-states.tokens.json');
const navigation = require('../docs/design/navigation.tokens.json');
const output = path.join(root, 'apps/website/src/_app/styles/tokens.css');
const aliases = { 'color/background': 'bg', 'color/border': 'line' };
function cssValue(token) {
  const value = token.$value;
  if (token.$type === 'color') {
    assert.match(value.hex, /^#[0-9a-f]{6}([0-9a-f]{2})?$/i);
    assert.equal(value.colorSpace, 'srgb');
    const hexAlpha = value.hex.length === 9 ? parseInt(value.hex.slice(7, 9), 16) / 255 : 1;
    assert.ok(Math.abs(value.alpha - hexAlpha) < 1e-8);
    assert.equal(value.components.length, 3);
    value.components.forEach((component, i) =>
      assert.ok(
        Math.abs(component - parseInt(value.hex.slice(1 + i * 2, 3 + i * 2), 16) / 255) < 1e-8,
      ),
    );
    return value.hex.toLowerCase();
  }
  if (token.$type === 'fontFamily') {
    assert.equal(typeof value, 'string');
    assert.match(value, /^[a-z0-9 -]+$/i);
    return `'${value}'`;
  }
  assert.ok(['dimension', 'duration'].includes(token.$type));
  assert.ok(Number.isFinite(value.value) && value.value >= 0);
  assert.ok((token.$type === 'dimension' ? ['px'] : ['s', 'ms']).includes(value.unit));
  return `${value.value}${value.unit}`;
}
const declarations = [];
for (const [group, tokens] of Object.entries(foundation)) {
  for (const [name, token] of Object.entries(tokens)) {
    const key = aliases[`${group}/${name}`] || (group === 'color' ? name : `${group}-${name}`);
    assert.match(key, /^[a-z][a-z0-9-]*$/);
    declarations.push(`  --${key}: ${cssValue(token)};`);
  }
}
for (const [name, token] of Object.entries(controls.color)) {
  assert.match(name, /^[a-z][a-z0-9-]*$/);
  declarations.push(`  --${name === 'cyan' ? name : `control-${name}`}: ${cssValue(token)};`);
}
for (const [name, token] of Object.entries(navigation.color)) {
  assert.match(name, /^[a-z][a-z0-9-]*$/);
  declarations.push(`  --navigation-${name}: ${cssValue(token)};`);
}
function luminance(hex) {
  const channels = [1, 3, 5]
    .map((i) => parseInt(hex.slice(i, i + 2), 16) / 255)
    .map((v) => (v <= 0.04045 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4));
  return channels[0] * 0.2126 + channels[1] * 0.7152 + channels[2] * 0.0722;
}
const f = (name) => foundation.color[name].$value.hex;
const c = (name) => controls.color[name].$value.hex;
const contrasts = [];
function contrast(label, foreground, background, minimum) {
  const a = luminance(foreground),
    b = luminance(background);
  const ratio = (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
  assert.ok(ratio >= minimum, `${label}: ${ratio.toFixed(2)} < ${minimum}`);
  contrasts.push({ label, ratio: Number(ratio.toFixed(2)), minimum });
}
for (const stop of [
  'primary-start',
  'primary-end',
  'primary-hover-start',
  'primary-hover-end',
  'primary-pressed-start',
  'primary-pressed-end',
])
  contrast(`Primary text / ${stop}`, f('on-accent'), c(stop), 4.5);
for (const state of [
  'secondary-background',
  'secondary-hover',
  'secondary-pressed',
  'field-background',
])
  contrast(`Text / ${state}`, f('text'), c(state), 4.5);
contrast('Field border', c('border'), c('field-background'), 3);
contrast('Secondary border', c('border'), c('secondary-background'), 3);
contrast('Secondary hover border', f('accent'), c('secondary-hover'), 3);
contrast('Secondary pressed border', f('accent'), c('secondary-pressed'), 3);
contrast('Disabled text', c('disabled-text'), c('disabled-background'), 4.5);
contrast('Error text', c('danger'), c('field-background'), 4.5);
contrast('Navigation menu border', navigation.color['menu-border'].$value.hex, f('surface'), 3);
for (const background of ['background', 'surface', 'raised'])
  contrast(`Focus / ${background}`, f('accent'), f(background), 3);
const css = `/* Generated from docs/design token files by scripts/sync-website-tokens.cjs. */\n:root {\n${declarations.join('\n')}\n}\n`;
if (process.argv.includes('--check')) {
  assert.equal(
    fs.readFileSync(output, 'utf8').replaceAll('\r\n', '\n'),
    css,
    'Website tokens drifted; run npm run tokens:web',
  );
} else {
  fs.writeFileSync(output, css);
}
console.log(
  JSON.stringify({
    tokens: declarations.length,
    contrasts,
    generated: !process.argv.includes('--check'),
  }),
);
