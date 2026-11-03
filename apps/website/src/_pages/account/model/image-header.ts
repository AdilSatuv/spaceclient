/** Bounds browser raster allocation before creating a Blob URL. Full validation stays on the API. */
export function validPreviewHeader(bytes: Uint8Array, kind: 'skin' | 'cape') {
  if (
    bytes.length < 33 ||
    ![137, 80, 78, 71, 13, 10, 26, 10].every((byte, index) => bytes[index] === byte)
  )
    return false;
  const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  if (
    view.getUint32(8) !== 13 ||
    ![73, 72, 68, 82].every((byte, index) => bytes[index + 12] === byte)
  )
    return false;
  const width = view.getUint32(16),
    height = view.getUint32(20);
  return kind === 'skin'
    ? width === 64 && [32, 64].includes(height)
    : [64, 128, 256].includes(width) && height === width / 2;
}
