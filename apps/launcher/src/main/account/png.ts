import { inflateSync } from 'node:zlib';
import { crc32 } from 'node:zlib';
import { createHash } from 'node:crypto';
import { MAX_IMAGE_BYTES } from '@space/account-contracts';
import { UserFacingError } from '../application/user-facing-error.ts';

const invalid = () =>
  new UserFacingError(
    'PNG не прошёл проверку. Скин: 64×64 или classic 64×32; плащ: 64×32, 128×64 или 256×128, до 256 КиБ. Сохраните PNG без чересстрочной развёртки.',
  );
export function validatePng(bytes: Buffer, kind: 'skin' | 'cape', model = 'classic') {
  try {
    if (
      bytes.length > MAX_IMAGE_BYTES ||
      bytes.length < 57 ||
      !bytes.subarray(0, 8).equals(Buffer.from('89504e470d0a1a0a', 'hex'))
    )
      throw 0;
    let offset = 8,
      width = 0,
      height = 0,
      depth = 0,
      color = 0,
      chunks = 0,
      palette = 0,
      phase = 0,
      ended = false;
    const compressed: Buffer[] = [];
    while (offset < bytes.length) {
      if (++chunks > 128 || offset + 12 > bytes.length) throw 0;
      const length = bytes.readUInt32BE(offset),
        type = bytes.toString('ascii', offset + 4, offset + 8);
      if (
        !/^[A-Za-z]{4}$/.test(type) ||
        length > MAX_IMAGE_BYTES ||
        offset + 12 + length > bytes.length
      )
        throw 0;
      const data = bytes.subarray(offset + 8, offset + 8 + length);
      if (
        crc32(bytes.subarray(offset + 4, offset + 8 + length)) !==
        bytes.readUInt32BE(offset + 8 + length)
      )
        throw 0;
      if (chunks === 1 && type !== 'IHDR') throw 0;
      if (type === 'IHDR') {
        if (chunks !== 1 || length !== 13) throw 0;
        width = data.readUInt32BE(0);
        height = data.readUInt32BE(4);
        depth = data[8]!;
        color = data[9]!;
        if (
          kind === 'skin'
            ? width !== 64 ||
              ![32, 64].includes(height) ||
              !['classic', 'slim'].includes(model) ||
              (height === 32 && model !== 'classic')
            : ![64, 128, 256].includes(width) || height !== width / 2
        )
          throw 0;
        const depths: Record<number, number[]> = {
          0: [1, 2, 4, 8, 16],
          2: [8, 16],
          3: [1, 2, 4, 8],
          4: [8, 16],
          6: [8, 16],
        };
        if (!depths[color]?.includes(depth) || data[10] || data[11] || data[12]) throw 0;
      } else if (type === 'PLTE') {
        if (palette || phase || [0, 4].includes(color) || length < 3 || length > 768 || length % 3)
          throw 0;
        palette = length / 3;
        if (color === 3 && palette > 2 ** depth) throw 0;
      } else if (type === 'IDAT') {
        if (phase === 2 || (color === 3 && !palette)) throw 0;
        phase = 1;
        compressed.push(data);
      } else if (type === 'IEND') {
        if (!compressed.length || length !== 0 || offset + 12 !== bytes.length) throw 0;
        ended = true;
      } else {
        if (type[0] === type[0]!.toUpperCase() || ['acTL', 'fcTL', 'fdAT'].includes(type)) throw 0;
        if (phase === 1) phase = 2;
        if (
          type === 'tRNS' &&
          (phase ||
            (color === 3
              ? !palette || length > palette
              : color === 0
                ? length !== 2
                : color === 2
                  ? length !== 6
                  : true))
        )
          throw 0;
      }
      offset += length + 12;
    }
    if (!ended) throw 0;
    const channels = ({ 0: 1, 2: 3, 3: 1, 4: 2, 6: 4 } as Record<number, number>)[color]!;
    const row = Math.ceil((width * channels * depth) / 8),
      bpp = Math.max(1, Math.ceil((channels * depth) / 8));
    const input = Buffer.concat(compressed),
      expected = (row + 1) * height;
    // Node 24 supports info:true; the pinned @types/node overload predates its result type.
    const inflated = inflateSync(input, {
      maxOutputLength: expected + 1,
      info: true,
    }) as unknown as { buffer: Buffer; engine: { bytesWritten: number } };
    if (inflated.buffer.length !== expected || inflated.engine.bytesWritten !== input.length)
      throw 0;
    let previous: Buffer = Buffer.alloc(row);
    const paeth = (a: number, b: number, c: number) => {
      const p = a + b - c,
        pa = Math.abs(p - a),
        pb = Math.abs(p - b),
        pc = Math.abs(p - c);
      return pa <= pb && pa <= pc ? a : pb <= pc ? b : c;
    };
    for (let y = 0; y < height; y++) {
      const filter = inflated.buffer[y * (row + 1)]!;
      if (filter > 4) throw 0;
      const current = Buffer.from(inflated.buffer.subarray(y * (row + 1) + 1, (y + 1) * (row + 1)));
      for (let x = 0; x < row; x++) {
        const a = x >= bpp ? current[x - bpp]! : 0,
          b = previous[x]!,
          c = x >= bpp ? previous[x - bpp]! : 0;
        current[x] =
          (current[x]! +
            (filter === 1
              ? a
              : filter === 2
                ? b
                : filter === 3
                  ? Math.floor((a + b) / 2)
                  : filter === 4
                    ? paeth(a, b, c)
                    : 0)) &
          255;
      }
      if (color === 3)
        for (let x = 0; x < width; x++)
          if (
            ((current[Math.floor((x * depth) / 8)]! >> (8 - depth - ((x * depth) % 8))) &
              ((1 << depth) - 1)) >=
            palette
          )
            throw 0;
      previous = current;
    }
    return {
      sha256: createHash('sha256').update(bytes).digest('hex'),
      bytes: bytes.length,
      width,
      height,
    };
  } catch {
    throw invalid();
  }
}
