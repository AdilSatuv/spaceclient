import { deflateSync, inflateSync } from 'node:zlib';
import { MAX_IMAGE_BYTES } from '@space/account-contracts';

const signature = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);
export function crc32(bytes: Uint8Array) {
  let crc = 0xffffffff;
  for (const byte of bytes) {
    crc ^= byte;
    for (let bit = 0; bit < 8; bit++) crc = (crc >>> 1) ^ (crc & 1 ? 0xedb88320 : 0);
  }
  return (crc ^ 0xffffffff) >>> 0;
}
const invalid = (): never => {
  throw new Error('Invalid PNG');
};
const chunk = (type: string, bytes: Buffer) => {
  const out = Buffer.alloc(bytes.length + 12);
  out.writeUInt32BE(bytes.length);
  out.write(type, 4);
  bytes.copy(out, 8);
  out.writeUInt32BE(crc32(out.subarray(4, -4)), out.length - 4);
  return out;
};
const paeth = (a: number, b: number, c: number) => {
  const p = a + b - c,
    x = Math.abs(p - a),
    y = Math.abs(p - b),
    z = Math.abs(p - c);
  return x <= y && x <= z ? a : y <= z ? b : c;
};

/** Decode every pixel (including Adam7/palette), then emit metadata-free RGBA8 PNG. */
export function validatePng(bytes: Buffer, kind: 'skin' | 'cape', model?: string) {
  if (bytes.length > MAX_IMAGE_BYTES || !bytes.subarray(0, 8).equals(signature)) invalid();
  let offset = 8,
    width = 0,
    height = 0,
    depth = 0,
    color = 0,
    interlace = 0;
  let ended = false,
    idat = false,
    idatClosed = false;
  let palette: Buffer | undefined, transparency: Buffer | undefined;
  const compressed: Buffer[] = [];
  for (let count = 0; offset < bytes.length && count < 2048; count++) {
    if (offset + 12 > bytes.length) invalid();
    const length = bytes.readUInt32BE(offset),
      type = bytes.toString('ascii', offset + 4, offset + 8);
    if (
      !bytes
        .subarray(offset + 4, offset + 8)
        .every((c) => (c >= 65 && c <= 90) || (c >= 97 && c <= 122))
    )
      invalid();
    if (
      length > MAX_IMAGE_BYTES ||
      offset + 12 + length > bytes.length ||
      !/^[A-Za-z]{4}$/.test(type) ||
      type[2] !== type[2].toUpperCase()
    )
      invalid();
    const data = bytes.subarray(offset + 8, offset + 8 + length);
    if (
      crc32(bytes.subarray(offset + 4, offset + 8 + length)) !==
      bytes.readUInt32BE(offset + 8 + length)
    )
      invalid();
    if (count === 0 && type !== 'IHDR') invalid();
    if (type === 'IHDR') {
      if (count || length !== 13) invalid();
      width = data.readUInt32BE(0);
      height = data.readUInt32BE(4);
      depth = data[8];
      color = data[9];
      interlace = data[12];
      if (
        (kind === 'skin'
          ? width !== 64 ||
            ![32, 64].includes(height) ||
            !['classic', 'slim'].includes(model || '') ||
            (height === 32 && model !== 'classic')
          : ![64, 128, 256].includes(width) || height !== width / 2) ||
        data[10] ||
        data[11] ||
        interlace > 1
      )
        invalid();
      const depths: Record<number, number[]> = {
        0: [1, 2, 4, 8, 16],
        2: [8, 16],
        3: [1, 2, 4, 8],
        4: [8, 16],
        6: [8, 16],
      };
      if (!depths[color]?.includes(depth)) invalid();
    } else if (type === 'PLTE') {
      if (
        palette ||
        idat ||
        transparency ||
        [0, 4].includes(color) ||
        !length ||
        length % 3 ||
        length > 768 ||
        (color === 3 && length / 3 > 2 ** depth)
      )
        invalid();
      palette = data;
    } else if (type === 'tRNS') {
      if (
        transparency ||
        idat ||
        (color === 0
          ? length !== 2
          : color === 2
            ? length !== 6
            : color === 3
              ? !palette || !length || length > palette.length / 3
              : true)
      )
        invalid();
      transparency = data;
    } else if (type === 'IDAT') {
      if (idatClosed || (color === 3 && !palette)) invalid();
      idat = true;
      compressed.push(data);
    } else if (type === 'IEND') {
      if (!idat || length || offset + 12 !== bytes.length) invalid();
      ended = true;
    } else {
      // Ancillary metadata is CRC checked and discarded, never passed to a browser/decoder.
      if (type[0] === type[0].toUpperCase() || ['acTL', 'fcTL', 'fdAT'].includes(type)) invalid();
    }
    if (idat && type !== 'IDAT') idatClosed = true;
    offset += 12 + length;
  }
  if (!ended || offset !== bytes.length) invalid();
  const channels = ({ 0: 1, 2: 3, 3: 1, 4: 2, 6: 4 } as Record<number, number>)[color];
  const passes = interlace
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
  const sizes = passes.map(([x, y, dx, dy]) => ({
    x,
    y,
    dx,
    dy,
    w: Math.max(0, Math.ceil((width - x) / dx)),
    h: Math.max(0, Math.ceil((height - y) / dy)),
  }));
  const expected = sizes.reduce(
    (sum, p) => sum + (p.w && p.h ? p.h * (1 + Math.ceil((p.w * channels * depth) / 8)) : 0),
    0,
  );
  const input = Buffer.concat(compressed);
  const inflated = inflateSync(input, { maxOutputLength: expected, info: true }) as unknown as {
    buffer: Buffer;
    engine: { bytesWritten: number };
  };
  if (inflated.buffer.length !== expected || inflated.engine.bytesWritten !== input.length)
    invalid();
  const rgba = Buffer.alloc(width * height * 4);
  let position = 0;
  for (const p of sizes) {
    if (!p.w || !p.h) continue;
    const rowBytes = Math.ceil((p.w * channels * depth) / 8),
      bpp = Math.max(1, Math.ceil((channels * depth) / 8));
    let previous = Buffer.alloc(rowBytes);
    for (let row = 0; row < p.h; row++) {
      const filter = inflated.buffer[position++];
      if (filter > 4) invalid();
      const current = Buffer.from(inflated.buffer.subarray(position, position + rowBytes));
      position += rowBytes;
      for (let i = 0; i < rowBytes; i++) {
        const a = i >= bpp ? current[i - bpp] : 0,
          b = previous[i],
          c = i >= bpp ? previous[i - bpp] : 0;
        current[i] =
          (current[i] +
            (filter === 0
              ? 0
              : filter === 1
                ? a
                : filter === 2
                  ? b
                  : filter === 3
                    ? Math.floor((a + b) / 2)
                    : paeth(a, b, c))) &
          255;
      }
      const sample = (index: number) =>
        depth === 16
          ? current.readUInt16BE(index * 2)
          : depth === 8
            ? current[index]
            : (current[Math.floor((index * depth) / 8)] >> (8 - depth - ((index * depth) % 8))) &
              ((1 << depth) - 1);
      const scale = (value: number) => Math.round((value * 255) / (2 ** depth - 1));
      for (let col = 0; col < p.w; col++) {
        const samples = Array.from({ length: channels }, (_, c) => sample(col * channels + c));
        let r: number,
          g: number,
          b: number,
          a = 255;
        if (color === 3) {
          const i = samples[0];
          if (!palette || i >= palette.length / 3) invalid();
          r = palette![i * 3];
          g = palette![i * 3 + 1];
          b = palette![i * 3 + 2];
          a = transparency?.[i] ?? 255;
        } else if (color === 0 || color === 4) {
          r = g = b = scale(samples[0]);
          if (color === 4) a = scale(samples[1]);
          else if (transparency && samples[0] === transparency.readUInt16BE(0)) a = 0;
        } else {
          r = scale(samples[0]);
          g = scale(samples[1]);
          b = scale(samples[2]);
          if (color === 6) a = scale(samples[3]);
          else if (transparency && samples.every((s, i) => s === transparency!.readUInt16BE(i * 2)))
            a = 0;
        }
        rgba.set([r!, g!, b!, a], ((p.y + row * p.dy) * width + p.x + col * p.dx) * 4);
      }
      previous = current;
    }
  }
  const header = Buffer.alloc(13);
  header.writeUInt32BE(width);
  header.writeUInt32BE(height, 4);
  header[8] = 8;
  header[9] = 6;
  const rows = Buffer.alloc(height * (width * 4 + 1));
  for (let y = 0; y < height; y++)
    rgba.copy(rows, y * (width * 4 + 1) + 1, y * width * 4, (y + 1) * width * 4);
  const png = Buffer.concat([
    signature,
    chunk('IHDR', header),
    chunk('IDAT', deflateSync(rows)),
    chunk('IEND', Buffer.alloc(0)),
  ]);
  if (png.length > MAX_IMAGE_BYTES) invalid();
  return { png, width, height };
}
