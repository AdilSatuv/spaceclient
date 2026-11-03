import { UserFacingError } from './user-facing-error.ts';
import type {
  SpaceCredentials,
  SpaceRegistration,
  SpaceProfileEdit,
  ImageSelection,
} from '../../shared/account.ts';

export function fields(value: unknown, keys: string[]): Record<string, unknown> {
  if (
    !value ||
    typeof value !== 'object' ||
    Array.isArray(value) ||
    Object.keys(value).some((k) => !keys.includes(k))
  )
    throw new UserFacingError('Неверные поля действия.');
  return value as Record<string, unknown>;
}
export function credentials(value: unknown, registration: true): SpaceRegistration;
export function credentials(value: unknown, registration?: false): SpaceCredentials;
export function credentials(
  value: unknown,
  registration = false,
): SpaceCredentials | SpaceRegistration {
  const source = fields(
    value,
    registration ? ['username', 'password', 'nickname'] : ['username', 'password'],
  );
  if (
    typeof source.username !== 'string' ||
    !/^[A-Za-z0-9_]{3,24}$/.test(source.username) ||
    typeof source.password !== 'string' ||
    [...source.password].length < 15 ||
    [...source.password].length > 128
  )
    throw new UserFacingError(
      'Имя Space: 3–24 латинских символа, цифры или _. Пароль: 15–128 символов.',
    );
  const result = { username: source.username, password: source.password };
  if (!registration) return result;
  if (typeof source.nickname !== 'string' || !/^[A-Za-z0-9_]{3,16}$/.test(source.nickname))
    throw new UserFacingError('Игровой ник: 3–16 латинских букв, цифр или _.');
  return { ...result, nickname: source.nickname };
}
export function revision(value: unknown): number {
  if (!Number.isSafeInteger(value) || Number(value) < 1)
    throw new UserFacingError('Обновите профиль Space перед изменением.');
  return Number(value);
}
export function profileEdit(value: unknown): SpaceProfileEdit {
  const source = fields(value, ['nickname', 'expectedRevision']);
  if (typeof source.nickname !== 'string' || !/^[A-Za-z0-9_]{3,16}$/.test(source.nickname))
    throw new UserFacingError('Игровой ник: 3–16 латинских букв, цифр или _.');
  return { nickname: source.nickname, expectedRevision: revision(source.expectedRevision) };
}
export function imageSelection(value: unknown, kind: 'skin' | 'cape'): ImageSelection {
  const source = fields(
    value,
    kind === 'skin' ? ['expectedRevision', 'model'] : ['expectedRevision'],
  );
  if (kind === 'skin' && source.model !== 'classic' && source.model !== 'slim')
    throw new UserFacingError('Выберите classic или slim.');
  return {
    expectedRevision: revision(source.expectedRevision),
    ...(kind === 'skin' ? { model: source.model as 'classic' | 'slim' } : {}),
  };
}
