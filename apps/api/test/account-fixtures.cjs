const { deflateSync } = require('node:zlib');
const { crc32 } = require('../dist/accounts/png.js');
const chunk = (type, data) => {
  const out = Buffer.alloc(data.length + 12);
  out.writeUInt32BE(data.length);
  out.write(type, 4);
  data.copy(out, 8);
  out.writeUInt32BE(crc32(out.subarray(4, -4)), out.length - 4);
  return out;
};
const signature = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);
function png(width = 64, height = 64, options = {}) {
  const header = Buffer.alloc(13);
  header.writeUInt32BE(width);
  header.writeUInt32BE(height, 4);
  header[8] = options.depth ?? 8;
  header[9] = options.color ?? 6;
  header[12] = options.interlace ?? 0;
  const channels = { 0: 1, 2: 3, 3: 1, 4: 2, 6: 4 }[header[9]];
  const passes = header[12]
    ? [
        [0, 0, 8, 8],
        [4, 0, 8, 8],
        [0, 4, 4, 8],
        [2, 0, 4, 4],
        [0, 2, 2, 4],
        [1, 0, 2, 2],
        [0, 1, 1, 2],
      ]
    : [[0, 0, 1, 1]];
  const blocks = [];
  for (const [x, y, dx, dy] of passes) {
    const w = Math.max(0, Math.ceil((width - x) / dx)),
      h = Math.max(0, Math.ceil((height - y) / dy));
    if (!w || !h) continue;
    const row = 1 + Math.ceil((w * channels * header[8]) / 8);
    const raw = Buffer.alloc(row * h);
    for (let i = 0; i < h; i++) raw[i * row] = options.filter ?? 0;
    blocks.push(raw);
  }
  const raw = options.raw ?? Buffer.concat(blocks);
  return Buffer.concat([
    signature,
    chunk('IHDR', header),
    ...(options.palette ? [chunk('PLTE', options.palette)] : []),
    chunk('IDAT', options.compressed ?? deflateSync(raw)),
    chunk('IEND', Buffer.alloc(0)),
  ]);
}
module.exports = { png, chunk, signature };
