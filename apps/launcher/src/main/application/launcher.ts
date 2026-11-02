import type { CommandHandlers } from '../../shared/ipc.ts';
import type { LauncherDependencies } from './ports.ts';
import type { JavaInfo } from '../../shared/models.ts';
import { UserFacingError } from './user-facing-error.ts';

export class LauncherApplication {
  readonly commands: CommandHandlers;
  private readonly dependencies: LauncherDependencies;
  private checkingJava: { controller: AbortController; done: Promise<JavaInfo> } | null = null;
  private choosingJava = false;
  private closing = false;

  constructor(dependencies: LauncherDependencies) {
    this.dependencies = dependencies;
    const { profiles, installation, launch } = dependencies;
    const noPayload = (payload: unknown) => {
      if (payload !== undefined) throw new UserFacingError('Действие не принимает параметры.');
    };
    this.commands = {
      'local:save': (payload) => profiles.saveNickname(payload),
      'launch:get': (payload) => launch.status(profiles.profile(payload).id),
      'launch:start': (payload) => {
        const profile = profiles.profile(payload);
        if (installation.running)
          throw new UserFacingError('Дождитесь завершения подготовки файлов.');
        return launch.start(profile, profiles.snapshot().localNickname);
      },
      'state:get': (payload) => {
        noPayload(payload);
        return profiles.snapshot();
      },
      'profile:save': (payload) => profiles.save(payload),
      'profile:create': (payload) => {
        noPayload(payload);
        return profiles.create();
      },
      'profile:select': (payload) => profiles.select(payload),
      'java:choose': (payload) => this.chooseJava(payload),
      'java:check': (payload) => this.checkJava(payload),
      'profile:open': async (payload) => {
        const path = await profiles.instanceDirectory(payload);
        await dependencies.desktop.openDirectory(path);
        return null;
      },
      'installation:get': (payload) => installation.status(profiles.profile(payload).id),
      'installation:start': (payload) => {
        const id = profiles.profile(payload).id;
        if (launch.running)
          throw new UserFacingError('Сначала закройте игру и дождитесь завершения запуска.');
        return installation.start(id);
      },
      'installation:cancel': (payload) => installation.cancel(profiles.profile(payload).id),
    };
  }
  get running() {
    return (
      this.dependencies.installation.running ||
      this.dependencies.launch.running ||
      this.checkingJava !== null
    );
  }
  async shutdown() {
    this.closing = true;
    const check = this.checkingJava;
    check?.controller.abort();
    await Promise.all([
      this.dependencies.installation.shutdown(),
      this.dependencies.launch.shutdown(),
      check?.done.catch(() => {}),
    ]);
  }

  private async chooseJava(payload: unknown) {
    if (this.choosingJava) throw new UserFacingError('Выбор Java уже открыт.');
    const { profiles, desktop } = this.dependencies;
    const profile = profiles.profile(payload);
    this.choosingJava = true;
    try {
      const selected = await desktop.chooseJava();
      return selected === null ? profiles.snapshot() : await profiles.setJava(profile.id, selected);
    } finally {
      this.choosingJava = false;
    }
  }
  private async checkJava(payload: unknown) {
    if (this.closing) throw new UserFacingError('Лаунчер завершает работу.');
    if (this.checkingJava) throw new UserFacingError('Проверка Java уже выполняется.');
    const profile = this.dependencies.profiles.profile(payload);
    const controller = new AbortController();
    const check = {
      controller,
      done: Promise.resolve().then(() =>
        this.dependencies.inspectJava(profile.javaPath, controller.signal),
      ),
    };
    this.checkingJava = check;
    try {
      return await check.done;
    } finally {
      if (this.checkingJava === check) this.checkingJava = null;
    }
  }
}
