import { USERNAME_PATTERN, NICKNAME_PATTERN, MAX_IMAGE_BYTES } from '@space/account-contracts';
import { accountError } from './errors';
export const badInput = () => accountError(400, 'SPACE_INVALID_INPUT', 'Проверьте поля формы.');
export function fields(value: unknown, required: string[], optional: string[] = []) {
  if (!value || typeof value !== 'object' || Array.isArray(value)) return badInput();
  const x = value as Record<string, unknown>;
  if (
    required.some((k) => !Object.hasOwn(x, k)) ||
    Object.keys(x).some((k) => ![...required, ...optional].includes(k))
  )
    return badInput();
  return x;
}
export function credentials(value: unknown, registration = false) {
  const x = fields(
    value,
    registration ? ['username', 'password', 'nickname'] : ['username', 'password'],
  );
  if (
    typeof x.username !== 'string' ||
    !USERNAME_PATTERN.test(x.username) ||
    typeof x.password !== 'string' ||
    [...x.password].length < 15 ||
    [...x.password].length > 128 ||
    x.password.includes('\u0000')
  )
    return badInput();
  if (registration && (typeof x.nickname !== 'string' || !NICKNAME_PATTERN.test(x.nickname)))
    return badInput();
  return { username: x.username, password: x.password, nickname: x.nickname as string };
}
export function revision(value: unknown) {
  if (!Number.isSafeInteger(value) || Number(value) < 1) return badInput();
  return Number(value);
}
export function imageInput(value: unknown, kind: 'skin' | 'cape') {
  const x = fields(
    value,
    kind === 'skin'
      ? ['pngBase64', 'expectedRevision', 'model']
      : ['pngBase64', 'expectedRevision'],
  );
  const rev = revision(x.expectedRevision);
  if (
    typeof x.pngBase64 !== 'string' ||
    x.pngBase64.length > Math.ceil(MAX_IMAGE_BYTES / 3) * 4 ||
    !x.pngBase64.length ||
    !/^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$/.test(x.pngBase64) ||
    (kind === 'skin' && !['classic', 'slim'].includes(String(x.model)))
  )
    return badInput();
  const bytes = Buffer.from(x.pngBase64, 'base64');
  if (bytes.toString('base64') !== x.pngBase64 || bytes.length > MAX_IMAGE_BYTES) return badInput();
  return { bytes, rev, model: x.model as 'classic' | 'slim' | undefined };
}
