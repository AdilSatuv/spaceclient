import type { CommandHandlers } from '../../shared/ipc.ts';
import type { LauncherDependencies } from './ports.ts';
import type { JavaInfo } from '../../shared/models.ts';
import { UserFacingError } from './user-facing-error.ts';
import { credentials, profileEdit, imageSelection, revision, fields } from './account-input.ts';

export class LauncherApplication {
  readonly commands: CommandHandlers;
  private readonly dependencies: LauncherDependencies;
  private checkingJava: { controller: AbortController; done: Promise<JavaInfo> } | null = null;
  private choosingJava = false;
  private closing = false;
  private maintenance: Promise<unknown> | null = null;
  private profileChange: Promise<unknown> | null = null;

  constructor(dependencies: LauncherDependencies) {
    this.dependencies = dependencies;
    const { profiles, installation, launch } = dependencies;
    const noPayload = (payload: unknown) => {
      if (payload !== undefined) throw new UserFacingError('Действие не принимает параметры.');
    };
    this.commands = {
      'account:get': (payload) => {
        noPayload(payload);
        return this.account().status();
      },
      'account:register': (payload) => this.account().register(credentials(payload, true)),
      'account:login': (payload) => this.account().login(credentials(payload)),
      'account:logout': (payload) => {
        noPayload(payload);
        return this.account().logout();
      },
      'account:edit': (payload) => this.account().edit(profileEdit(payload)),
      'account:skin:upload': (payload) =>
        this.account().upload('skin', imageSelection(payload, 'skin')),
      'account:cape:upload': (payload) =>
        this.account().upload('cape', imageSelection(payload, 'cape')),
      'account:skin:delete': (payload) => this.account().remove('skin', revision(payload)),
      'account:cape:delete': (payload) => this.account().remove('cape', revision(payload)),
      'account:apply': (payload) => {
        const value = fields(payload, ['id', 'confirmIdentityChange']);
        const id = profiles.profile(value.id).id;
        if (value.confirmIdentityChange !== true)
          throw new UserFacingError('Подтвердите возможную смену offline identity.');
        return this.maintain(async () => ({
          account: await this.account().apply(id),
          snapshot: profiles.snapshot(),
        }));
      },
      'profile:backup': (payload) => {
        const id = profiles.profile(payload).id;
        return this.maintain(() => this.recovery().backup(id));
      },
      'profile:restore': (payload) => {
        const value = fields(payload, ['id', 'confirmed']),
          id = profiles.profile(value.id).id;
        if (value.confirmed !== true)
          throw new UserFacingError('Подтвердите восстановление настроек.');
        return this.maintain(async () => ({
          result: await this.recovery().restore(id),
          snapshot: profiles.snapshot(),
        }));
      },
      'diagnostics:export': (payload) => {
        const id = profiles.profile(payload).id;
        return this.maintain(() => this.recovery().diagnostics(id));
      },
      'local:save': (payload) => this.changeProfile(() => profiles.saveNickname(payload)),
      'launch:get': (payload) => launch.status(profiles.profile(payload).id),
      'launch:start': (payload) => {
        this.available();
        const profile = profiles.profile(payload);
        if (installation.running)
          throw new UserFacingError('Дождитесь завершения подготовки файлов.');
        return launch.start(profile, profiles.snapshot().localNickname);
      },
      'state:get': (payload) => {
        noPayload(payload);
        return profiles.snapshot();
      },
      'profile:save': (payload) => this.changeProfile(() => profiles.save(payload)),
      'profile:create': (payload) => {
        noPayload(payload);
        return this.changeProfile(() => profiles.create());
      },
      'profile:select': (payload) => this.changeProfile(() => profiles.select(payload)),
      'java:choose': (payload) => this.chooseJava(payload),
      'java:check': (payload) => this.checkJava(payload),
      'profile:open': async (payload) => {
        const path = await profiles.instanceDirectory(payload);
        await dependencies.desktop.openDirectory(path);
        return null;
      },
      'installation:get': (payload) => installation.status(profiles.profile(payload).id),
      'installation:start': (payload) => {
        this.available();
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
      this.maintenance !== null ||
      this.profileChange !== null ||
      this.dependencies.account?.running ||
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
      this.maintenance?.catch(() => {}),
      this.profileChange?.catch(() => {}),
      this.dependencies.account?.settled(),
    ]);
  }

  private account() {
    if (this.closing) throw new UserFacingError('Лаунчер завершает работу.');
    if (!this.dependencies.account)
      throw new UserFacingError('Аккаунты Space недоступны в этой сборке.');
    return this.dependencies.account;
  }
  private recovery() {
    if (this.closing) throw new UserFacingError('Лаунчер завершает работу.');
    if (!this.dependencies.recovery)
      throw new UserFacingError('Восстановление недоступно в этой сборке.');
    return this.dependencies.recovery;
  }
  private available() {
    if (
      this.closing ||
      this.maintenance ||
      this.profileChange ||
      this.choosingJava ||
      this.dependencies.account?.running
    )
      throw new UserFacingError('Дождитесь завершения действия с аккаунтом или настройками.');
  }
  private async changeProfile<T>(operation: () => Promise<T>): Promise<T> {
    this.available();
    const task = Promise.resolve().then(operation);
    this.profileChange = task;
    try {
      return await task;
    } finally {
      if (this.profileChange === task) this.profileChange = null;
    }
  }
  private async maintain<T>(operation: () => Promise<T>): Promise<T> {
    this.available();
    if (this.dependencies.launch.running || this.dependencies.installation.running)
      throw new UserFacingError('Завершите игру, запуск и подготовку файлов перед этим действием.');
    const task = Promise.resolve().then(operation);
    this.maintenance = task;
    try {
      return await task;
    } finally {
      if (this.maintenance === task) this.maintenance = null;
    }
  }

  private async chooseJava(payload: unknown) {
    if (this.choosingJava) throw new UserFacingError('Выбор Java уже открыт.');
    this.available();
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
