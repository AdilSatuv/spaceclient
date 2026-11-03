const test = require('node:test');
const assert = require('node:assert/strict');
test('account preview rejects oversized bitmap headers before browser image decoding', async () => {
  const { validPreviewHeader } =
    await import('../apps/website/src/_pages/account/model/image-header.ts');
  const header = (w, h) => {
    const bytes = new Uint8Array(33);
    bytes.set([137, 80, 78, 71, 13, 10, 26, 10]);
    bytes.set([73, 72, 68, 82], 12);
    const view = new DataView(bytes.buffer);
    view.setUint32(8, 13);
    view.setUint32(16, w);
    view.setUint32(20, h);
    return bytes;
  };
  for (const [w, h] of [
    [64, 64],
    [64, 32],
  ])
    assert.equal(validPreviewHeader(header(w, h), 'skin'), true);
  for (const [w, h] of [
    [64, 32],
    [128, 64],
    [256, 128],
  ])
    assert.equal(validPreviewHeader(header(w, h), 'cape'), true);
  for (const [w, h] of [
    [100000, 100000],
    [64, 2147483647],
    [0, 0],
    [63, 64],
    [128, 128],
  ])
    assert.equal(validPreviewHeader(header(w, h), 'skin'), false);
  const invalid = header(64, 64);
  invalid[8] = 1;
  assert.equal(validPreviewHeader(invalid, 'skin'), false);
  assert.equal(validPreviewHeader(new Uint8Array(12), 'skin'), false);
  assert.equal(validPreviewHeader(new Uint8Array(33), 'cape'), false);
});
