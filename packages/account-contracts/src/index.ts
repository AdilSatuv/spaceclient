/** Space account data only; Minecraft authentication and Electron privileges are separate. */
export const USERNAME_PATTERN = /^[a-zA-Z0-9_]{3,24}$/;
export const NICKNAME_PATTERN = /^[a-zA-Z0-9_]{3,16}$/;
export const MAX_IMAGE_BYTES = 256 * 1024;
export const MIN_PASSWORD_LENGTH = 15;
export const MAX_PASSWORD_LENGTH = 128;
export type SkinModel = 'classic' | 'slim';
export type PngAsset = { sha256: string; bytes: number; width: number; height: number };
export type SkinAsset = PngAsset & { model: SkinModel };
export type SpaceAccountProfile = {
  id: string;
  username: string;
  nickname: string;
  revision: number;
  createdAt: string;
  updatedAt: string;
  skin: SkinAsset | null;
  cape: PngAsset | null;
};
/** Server/main only: never forward sessionToken to an Electron renderer or browser JSON. */
export type SpaceAccountSession = {
  profile: SpaceAccountProfile;
  sessionToken: string;
  expiresAt: string;
};
export type SpaceCredentials = { username: string; password: string };
export type SpaceRegistration = SpaceCredentials & { nickname: string };
export type SpaceProfileEdit = { nickname: string; expectedRevision: number };
export type SpaceImageEdit = { pngBase64: string; expectedRevision: number; model?: SkinModel };

const record = (value: unknown): Record<string, unknown> => {
  if (!value || typeof value !== 'object' || Array.isArray(value))
    throw new Error('Invalid account response');
  return value as Record<string, unknown>;
};
const date = (value: unknown): string => {
  if (typeof value !== 'string' || value.length > 40 || !Number.isFinite(Date.parse(value)))
    throw new Error('Invalid account response');
  return value;
};
function image(value: unknown, kind: 'skin' | 'cape'): SkinAsset | PngAsset | null {
  if (value === null) return null;
  const source = record(value);
  if (
    typeof source.sha256 !== 'string' ||
    !/^[a-f0-9]{64}$/.test(source.sha256) ||
    !Number.isInteger(source.bytes) ||
    Number(source.bytes) < 1 ||
    Number(source.bytes) > MAX_IMAGE_BYTES ||
    !Number.isInteger(source.width) ||
    !Number.isInteger(source.height)
  )
    throw new Error('Invalid account response');
  const width = Number(source.width),
    height = Number(source.height);
  if (
    kind === 'skin'
      ? width !== 64 || ![32, 64].includes(height)
      : ![64, 128, 256].includes(width) || height !== width / 2
  )
    throw new Error('Invalid account response');
  const result = { sha256: source.sha256, bytes: Number(source.bytes), width, height };
  if (kind === 'cape') return result;
  if (
    !['classic', 'slim'].includes(String(source.model)) ||
    (height === 32 && source.model !== 'classic')
  )
    throw new Error('Invalid account response');
  return { ...result, model: source.model as SkinModel };
}
/** Validate and copy an explicit public allowlist, never a server storage object. */
export function readSpaceAccountProfile(value: unknown): SpaceAccountProfile {
  const source = record(value);
  if (
    typeof source.id !== 'string' ||
    !/^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/.test(source.id) ||
    typeof source.username !== 'string' ||
    !USERNAME_PATTERN.test(source.username) ||
    typeof source.nickname !== 'string' ||
    !NICKNAME_PATTERN.test(source.nickname) ||
    !Number.isSafeInteger(source.revision) ||
    Number(source.revision) < 1
  )
    throw new Error('Invalid account response');
  return {
    id: source.id,
    username: source.username,
    nickname: source.nickname,
    revision: Number(source.revision),
    createdAt: date(source.createdAt),
    updatedAt: date(source.updatedAt),
    skin: image(source.skin, 'skin') as SkinAsset | null,
    cape: image(source.cape, 'cape'),
  };
}
export function readSpaceAccountSession(value: unknown): SpaceAccountSession {
  const source = record(value);
  if (
    typeof source.sessionToken !== 'string' ||
    !/^[a-zA-Z0-9_-]{43,256}$/.test(source.sessionToken)
  )
    throw new Error('Invalid account response');
  return {
    profile: readSpaceAccountProfile(source.profile),
    sessionToken: source.sessionToken,
    expiresAt: date(source.expiresAt),
  };
}
