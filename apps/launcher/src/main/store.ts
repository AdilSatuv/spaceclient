import { readFile, writeFile, mkdir, rename, copyFile, lstat } from 'node:fs/promises';
import { constants } from 'node:fs';
import { join } from 'node:path';
import { randomUUID } from 'node:crypto';
import type { SavedState, Snapshot } from '../shared/contracts.ts';
import {
  savedState,
  editProfile,
  profileId,
  javaPath,
  memoryLimit,
  MAX_PROFILES,
} from './validation.ts';

export class ProfileStore {
  readonly root: string;
  readonly maxMemoryMb: number;
  readonly totalMemoryMb: number;
  private state: SavedState;
  private warning: string | null = null;
  private queue: Promise<unknown> = Promise.resolve();
  private constructor(root: string, totalMemoryMb: number) {
    this.root = root;
    this.totalMemoryMb = totalMemoryMb;
    this.maxMemoryMb = memoryLimit(totalMemoryMb);
    this.state = {
      schemaVersion: 1,
      activeProfileId: 'default',
      profiles: [
        {
          id: 'default',
          name: 'Моя орбита',
          memoryMb: Math.min(2048, this.maxMemoryMb),
          javaPath: null,
        },
      ],
    };
  }
  static async open(root: string, totalMemoryMb: number): Promise<ProfileStore> {
    const store = new ProfileStore(root, totalMemoryMb);
    await mkdir(root, { recursive: true });
    let raw: string;
    try {
      raw = await readFile(join(root, 'settings.json'), 'utf8');
    } catch (error) {
      if ((error as NodeJS.ErrnoException).code === 'ENOENT') return store;
      throw new Error('Не удалось прочитать настройки. Проверьте доступ к папке лаунчера.');
    }
    try {
      store.state = savedState(JSON.parse(raw), store.maxMemoryMb);
    } catch {
      // Keep the original on disk until a recovery copy has safely been created.
      try {
        await copyFile(
          join(root, 'settings.json'),
          join(root, `settings.invalid-${Date.now()}-${randomUUID()}.json`),
          constants.COPYFILE_EXCL,
        );
      } catch {
        throw new Error(
          'Настройки повреждены, а резервную копию сохранить не удалось. Исходный файл оставлен без изменений.',
        );
      }
      store.warning =
        'Настройки не удалось загрузить. Исходный файл сохранён в резервной копии; открыт новый профиль.';
    }
    return store;
  }
  snapshot(): Snapshot {
    return {
      ...structuredClone(this.state),
      maxMemoryMb: this.maxMemoryMb,
      totalMemoryMb: this.totalMemoryMb,
      dataRoot: this.root,
      warning: this.warning,
    };
  }
  profile(id: unknown) {
    const profile = this.state.profiles.find((item) => item.id === profileId(id));
    if (!profile) throw new Error('Профиль не найден.');
    return { ...profile };
  }
  private mutate(change: (state: SavedState) => void): Promise<Snapshot> {
    const operation = this.queue.then(async () => {
      const next = structuredClone(this.state);
      change(next);
      const verified = savedState(next, this.maxMemoryMb);
      const temporary = join(this.root, `settings-${randomUUID()}.tmp`);
      try {
        await writeFile(temporary, JSON.stringify(verified, null, 2), { flag: 'wx', mode: 0o600 });
        await rename(temporary, join(this.root, 'settings.json'));
      } catch {
        throw new Error('Не удалось сохранить настройки. Изменения не применены.');
      }
      this.state = verified;
      return this.snapshot();
    });
    this.queue = operation.catch(() => {});
    return operation;
  }
  save(edit: unknown) {
    const parsed = editProfile(edit, this.maxMemoryMb);
    return this.mutate((state) => {
      const target = state.profiles.find((profile) => profile.id === parsed.id);
      if (!target) throw new Error('Профиль не найден.');
      Object.assign(target, parsed);
    });
  }
  create() {
    return this.mutate((state) => {
      if (state.profiles.length >= MAX_PROFILES)
        throw new Error('Можно создать не больше 10 профилей.');
      const id = randomUUID();
      state.profiles.push({
        id,
        name: `Орбита ${state.profiles.length + 1}`,
        memoryMb: Math.min(2048, this.maxMemoryMb),
        javaPath: null,
      });
      state.activeProfileId = id;
    });
  }
  select(id: unknown) {
    const target = this.profile(id);
    return this.mutate((state) => {
      state.activeProfileId = target.id;
    });
  }
  setJava(id: unknown, path: unknown) {
    const target = this.profile(id);
    const selected = javaPath(path);
    return this.mutate((state) => {
      state.profiles.find((profile) => profile.id === target.id)!.javaPath = selected;
    });
  }
  async instanceDirectory(id: unknown): Promise<string> {
    const profile = this.profile(id);
    const instances = join(this.root, 'instances');
    const directory = join(instances, profile.id);
    for (const path of [instances, directory]) {
      try {
        if ((await lstat(path)).isSymbolicLink())
          throw new Error('Каталог профиля не может быть ссылкой.');
      } catch (error) {
        if ((error as NodeJS.ErrnoException).code !== 'ENOENT') throw error;
      }
      await mkdir(path, { recursive: true });
    }
    return directory;
  }
}
