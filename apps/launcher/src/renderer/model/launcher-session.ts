import type {
  LauncherApi,
  Snapshot,
  Profile,
  ProfileEdit,
  JavaInfo,
  Result,
} from '../../shared/contracts.ts';

export type LauncherState = Readonly<{
  snapshot: Snapshot | null;
  profile: Profile | null;
  draft: ProfileEdit | null;
  java: Record<string, JavaInfo>;
  dirty: boolean;
  javaLabel: string;
  busy: boolean;
  error: string;
  notice: string;
}>;
export type ReadyLauncherModel = LauncherState & {
  snapshot: Snapshot;
  profile: Profile;
  draft: ProfileEdit;
  session: LauncherSession;
};

// Framework-independent presentation state. The main process still validates every command.
export class LauncherSession {
  private readonly api: LauncherApi;
  private readonly listeners = new Set<() => void>();
  private state: LauncherState = {
    snapshot: null,
    profile: null,
    draft: null,
    java: {},
    dirty: false,
    javaLabel: '',
    busy: false,
    error: '',
    notice: '',
  };
  constructor(api: LauncherApi) {
    this.api = api;
  }
  getSnapshot = (): LauncherState => this.state;
  subscribe = (listener: () => void): (() => void) => {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  };
  private update(change: Partial<LauncherState>) {
    const next = { ...this.state, ...change };
    const profile =
      next.snapshot?.profiles.find((item) => item.id === next.snapshot!.activeProfileId) ?? null;
    this.state = {
      ...next,
      profile,
      dirty: !!(
        next.draft &&
        profile &&
        (next.draft.name !== profile.name || next.draft.memoryMb !== profile.memoryMb)
      ),
      javaLabel: !profile
        ? ''
        : next.java[profile.id]?.usable
          ? 'Проверена · 21 x64'
          : profile.javaPath
            ? 'Нужна проверка'
            : 'Не выбрана',
    };
    for (const notify of [...this.listeners]) {
      try {
        notify();
      } catch {
        /* A view observer cannot turn a saved command into failure. */
      }
    }
  }
  private apply(value: Snapshot, notice = '', java = this.state.java) {
    const snapshot = structuredClone(value);
    const active = snapshot.profiles.find((item) => item.id === snapshot.activeProfileId);
    if (!active) throw new Error('Invalid snapshot');
    this.update({
      snapshot,
      draft: { id: active.id, name: active.name, memoryMb: active.memoryMb },
      notice,
      java,
    });
  }
  private async run<T>(
    action: () => Promise<Result<T>>,
    success: (value: T) => void,
  ): Promise<boolean> {
    if (this.state.busy) return false;
    this.update({ busy: true, error: '', notice: '' });
    try {
      const result = await action();
      if (!result.ok) {
        this.update({ error: result.error });
        return false;
      }
      success(result.value);
      return true;
    } catch {
      this.update({ error: 'Не удалось связаться с лаунчером. Перезапустите приложение.' });
      return false;
    } finally {
      this.update({ busy: false });
    }
  }
  private cleanDraft(): boolean {
    if (this.state.busy) return false;
    if (!this.state.dirty) return true;
    this.update({ error: 'Сначала сохраните или отмените изменения профиля.', notice: '' });
    return false;
  }
  dismissFeedback = () => {
    if (this.state.error || this.state.notice) this.update({ error: '', notice: '' });
  };
  editDraft = (change: Partial<Pick<ProfileEdit, 'name' | 'memoryMb'>>) => {
    const draft = this.state.draft;
    if (!draft || this.state.busy) return;
    this.update({
      draft: {
        id: draft.id,
        name: change.name ?? draft.name,
        memoryMb: change.memoryMb ?? draft.memoryMb,
      },
    });
  };
  cancelEdit = () => {
    if (!this.state.busy && this.state.snapshot) {
      this.apply(this.state.snapshot, 'Изменения отменены.');
      this.update({ error: '' });
    }
  };
  load = async () =>
    this.cleanDraft() &&
    this.run(
      () => this.api.getState(),
      (value) => this.apply(value),
    );
  saveProfile = async () => {
    if (!this.state.draft || !this.state.dirty) return false;
    const edit = { ...this.state.draft };
    return this.run(
      () => this.api.saveProfile(edit),
      (value) => this.apply(value, 'Профиль сохранён.'),
    );
  };
  createProfile = async () =>
    this.cleanDraft() &&
    this.run(
      () => this.api.createProfile(),
      (value) => this.apply(value, 'Новый профиль создан.'),
    );
  selectProfile = async (id: string) => {
    if (id === this.state.profile?.id) return false;
    return (
      this.cleanDraft() &&
      this.run(
        () => this.api.selectProfile(id),
        (value) => this.apply(value),
      )
    );
  };
  chooseJava = async () => {
    const profile = this.state.profile;
    if (!profile || !this.cleanDraft()) return false;
    return this.run(
      () => this.api.chooseJava(profile.id),
      (value) => {
        const java = { ...this.state.java };
        delete java[profile.id];
        this.apply(value, '', java);
      },
    );
  };
  checkJava = async () => {
    const profile = this.state.profile;
    if (!profile) return false;
    return this.run(
      () => this.api.checkJava(profile.id),
      (value) => {
        this.update({ java: { ...this.state.java, [profile.id]: value }, notice: value.message });
      },
    );
  };
  openFolder = async () => {
    const profile = this.state.profile;
    return (
      !!profile &&
      this.run(
        () => this.api.openInstance(profile.id),
        () => this.update({ notice: 'Папка профиля открыта.' }),
      )
    );
  };
}
