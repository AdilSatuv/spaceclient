import type { ReadyLauncherModel } from './launcher-session.ts';

type Inputs = Pick<
  ReadyLauncherModel,
  'profile' | 'snapshot' | 'busy' | 'dirty' | 'nicknameDirty' | 'launch'
>;

/** Explain renderer-side gates only; main validates Java and installed files at launch. */
export function launchAvailability(model: Inputs, preparing: boolean) {
  const current = model.launch?.profileId === model.profile.id ? model.launch : null;
  const running = !!current && ['checking', 'starting', 'running'].includes(current.phase);

  if (running)
    return {
      disabled: true,
      reason:
        current.phase === 'running'
          ? 'Игра уже работает для этого профиля.'
          : 'Дождитесь завершения запуска для этого профиля.',
    };
  if (preparing) return { disabled: true, reason: 'Дождитесь завершения подготовки файлов.' };
  if (model.busy) return { disabled: true, reason: 'Дождитесь завершения текущего действия.' };
  if (model.dirty) return { disabled: true, reason: 'Сохраните или отмените изменения профиля.' };
  if (model.nicknameDirty)
    return { disabled: true, reason: 'Введите корректный локальный ник и сохраните его.' };
  if (!model.snapshot.localNickname)
    return { disabled: true, reason: 'Введите и сохраните локальный ник.' };
  if (!current)
    return {
      disabled: true,
      reason:
        'Нет состояния запуска для этого профиля. Если ожидание затянулось, перезапустите лаунчер.',
    };
  return {
    disabled: false,
    reason: 'Можно попробовать запустить игру. Java и файлы будут проверены перед запуском.',
  };
}
