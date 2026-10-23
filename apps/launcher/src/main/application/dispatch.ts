import type {
  CommandName,
  CommandOutput,
  CommandHandler,
  CommandHandlers,
} from '../../shared/ipc.ts';
import type { Result } from '../../shared/models.ts';
import { UserFacingError } from './user-facing-error.ts';

export function createDispatcher<Event>(
  commands: CommandHandlers,
  authorized: (event: Event) => boolean,
) {
  return async <K extends CommandName>(
    channel: K,
    event: Event,
    payload: unknown,
  ): Promise<Result<CommandOutput<K>>> => {
    try {
      if (!Object.hasOwn(commands, channel) || !authorized(event))
        return { ok: false, error: 'Действие недоступно.' };
      const action = commands[channel] as CommandHandler<K>;
      return { ok: true, value: await action(payload) };
    } catch (error) {
      return {
        ok: false,
        error:
          error instanceof UserFacingError
            ? error.message
            : 'Не удалось выполнить действие. Повторите попытку.',
      };
    }
  };
}
