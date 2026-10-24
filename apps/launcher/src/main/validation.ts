import { UserFacingError } from './application/user-facing-error.ts';
import { win32 } from 'node:path';
import type { Profile, ProfileEdit, SavedState } from '../shared/contracts.ts';

export const MAX_PROFILES = 10;
export function nickname(value: unknown): string {
  if (typeof value !== 'string' || !/^[A-Za-z0-9_]{3,16}$/.test(value))
    throw new UserFacingError('Ник: от 3 до 16 латинских букв, цифр или знаков _.');
  return value;
}
export function memoryLimit(totalMb: number): number {
  return Math.max(1024, Math.min(16384, Math.floor((totalMb - 2048) / 256) * 256));
}
export function record(value: unknown): asserts value is Record<string, unknown> {
  if (!value || typeof value !== 'object' || Array.isArray(value))
    throw new UserFacingError('Неверный формат настроек.');
}
export function profileId(value: unknown): string {
  if (typeof value !== 'string' || !/^[a-z0-9][a-z0-9-]{0,47}$/.test(value))
    throw new UserFacingError('Неизвестный профиль.');
  return value;
}
export function editProfile(value: unknown, maxMemoryMb: number): ProfileEdit {
  record(value);
  if (Object.keys(value).some((key) => !['id', 'name', 'memoryMb'].includes(key)))
    throw new UserFacingError('Неподдерживаемое поле профиля.');
  const id = profileId(value.id);
  if (
    typeof value.name !== 'string' ||
    !value.name.trim() ||
    value.name.trim().length > 40 ||
    /[\x00-\x1f\x7f]/.test(value.name)
  )
    throw new UserFacingError('Имя должно содержать от 1 до 40 символов.');
  if (
    typeof value.memoryMb !== 'number' ||
    !Number.isInteger(value.memoryMb) ||
    value.memoryMb < 1024 ||
    value.memoryMb > maxMemoryMb ||
    value.memoryMb % 256 !== 0
  )
    throw new UserFacingError(`Выберите от 1024 до ${maxMemoryMb} МБ с шагом 256 МБ.`);
  return { id, name: value.name.trim(), memoryMb: value.memoryMb };
}
export function javaPath(value: unknown): string {
  if (
    typeof value !== 'string' ||
    value.length > 1024 ||
    !/^[a-z]:[\\/]/i.test(value) ||
    !win32.isAbsolute(value) ||
    win32.basename(value).toLowerCase() !== 'java.exe' ||
    value.includes('\0') ||
    value.startsWith('\\\\') ||
    value.startsWith('//')
  )
    throw new UserFacingError('Выберите локальный файл java.exe.');
  return value;
}
export function savedState(value: unknown, maxMemoryMb: number): SavedState {
  record(value);
  if (value.schemaVersion !== 1 && value.schemaVersion !== 2)
    throw new UserFacingError('Версия настроек не поддерживается.');
  const localNickname =
    value.schemaVersion === 1 || value.localNickname === null
      ? null
      : nickname(value.localNickname);
  if (
    !Array.isArray(value.profiles) ||
    value.profiles.length < 1 ||
    value.profiles.length > MAX_PROFILES
  )
    throw new UserFacingError('Неверный список профилей.');
  const profiles: Profile[] = value.profiles.map((raw) => {
    record(raw);
    const profile = editProfile(
      { id: raw.id, name: raw.name, memoryMb: raw.memoryMb },
      maxMemoryMb,
    );
    return { ...profile, javaPath: raw.javaPath === null ? null : javaPath(raw.javaPath) };
  });
  if (new Set(profiles.map((profile) => profile.id)).size !== profiles.length)
    throw new UserFacingError('Повторяющиеся профили.');
  const activeProfileId = profileId(value.activeProfileId);
  if (!profiles.some((profile) => profile.id === activeProfileId))
    throw new UserFacingError('Активный профиль не найден.');
  return { schemaVersion: 2, localNickname, activeProfileId, profiles };
}
