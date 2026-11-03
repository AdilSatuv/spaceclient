import { readSpaceAccountProfile, readSpaceAccountSession } from '@space/account-contracts';
import type {
  PngAsset,
  SpaceAccountSession,
  SpaceCredentials,
  SpaceRegistration,
  SpaceProfileEdit,
} from '@space/account-contracts';
import type { AccountState, ImageSelection } from '../../shared/account.ts';
import { UserFacingError } from '../application/user-facing-error.ts';
import { AccountHttp, AccountHttpError } from './http.ts';
import { SpaceSessionVault } from './session.ts';
import { validatePng } from './png.ts';
import { atomicFile, fileTransaction, readBounded } from '../bounded-files.ts';
import { installationTarget } from '../downloads.ts';

const APPEARANCE = 'config/space-appearance.properties';
type Dependencies = {
  rootForProfile(id: string): Promise<string>;
  localNickname(): string | null;
  saveNickname(name: string): Promise<unknown>;
  choosePng(kind: 'skin' | 'cape'): Promise<string | null>;
};
export class SpaceAccountService {
  private session: SpaceAccountSession | null | undefined;
  private previews: { skin: Buffer | null; cape: Buffer | null } = { skin: null, cape: null };
  private pending: Promise<unknown> | null = null;
  private readonly http: AccountHttp;
  private readonly vault: SpaceSessionVault;
  private readonly dependencies: Dependencies;
  constructor(http: AccountHttp, vault: SpaceSessionVault, dependencies: Dependencies) {
    this.http = http;
    this.vault = vault;
    this.dependencies = dependencies;
  }
  get running() {
    return this.pending !== null;
  }
  async settled() {
    await this.pending?.catch(() => {});
  }
  private async exclusive<T>(operation: () => Promise<T>): Promise<T> {
    if (this.pending) throw new UserFacingError('Дождитесь завершения действия с аккаунтом Space.');
    const task = Promise.resolve().then(operation);
    this.pending = task;
    try {
      return await task;
    } finally {
      if (this.pending === task) this.pending = null;
    }
  }
  private async current() {
    if (this.session === undefined) this.session = await this.vault.load();
    if (this.session && Date.parse(this.session.expiresAt) <= Date.now()) {
      await this.vault.clear();
      this.session = null;
      this.previews = { skin: null, cape: null };
    }
    return this.session;
  }
  private async required() {
    const value = await this.current();
    if (!value) throw new AccountHttpError('SPACE_UNAUTHORIZED');
    return value;
  }
  private state(message = ''): AccountState {
    return {
      profile: this.session ? readSpaceAccountProfile(this.session.profile) : null,
      expiresAt: this.session?.expiresAt ?? null,
      skinPreview: this.previews.skin?.toString('base64') ?? null,
      capePreview: this.previews.cape?.toString('base64') ?? null,
      message,
    };
  }
  private async request(
    path: string,
    method = 'GET',
    body?: unknown,
    session?: SpaceAccountSession,
  ) {
    try {
      return await this.http.request(path, method, body, session?.sessionToken);
    } catch (error) {
      if (error instanceof AccountHttpError && error.code === 'SPACE_UNAUTHORIZED') {
        await this.vault.clear();
        this.session = null;
        this.previews = { skin: null, cape: null };
      }
      throw error;
    }
  }
  private async asset(descriptor: PngAsset | null, kind: 'skin' | 'cape', model = 'classic') {
    if (!descriptor) return null;
    const bytes = (await this.http.request(
      '/v1/accounts/assets/' + descriptor.sha256,
      'GET',
      undefined,
      undefined,
      true,
    )) as Buffer;
    const verified = validatePng(bytes, kind, model);
    if (
      verified.sha256 !== descriptor.sha256 ||
      verified.bytes !== descriptor.bytes ||
      verified.width !== descriptor.width ||
      verified.height !== descriptor.height
    )
      throw new UserFacingError(
        'Изображение Space не совпало с проверенным описанием. Повторите загрузку профиля.',
      );
    return bytes;
  }
  private async updateProfile(value: unknown) {
    const session = await this.required();
    const profile = readSpaceAccountProfile((value as { profile: unknown })?.profile);
    if (profile.id !== session.profile.id || profile.username !== session.profile.username)
      throw new UserFacingError('Ответ Space не соответствует текущему аккаунту.');
    const skin = await this.asset(profile.skin, 'skin', profile.skin?.model),
      cape = await this.asset(profile.cape, 'cape');
    const next = { ...session, profile };
    await this.vault.save(next);
    this.session = next;
    this.previews = { skin, cape };
  }
  status() {
    return this.exclusive(async () => {
      const value = await this.current();
      if (value)
        await this.updateProfile(await this.request('/v1/accounts/me', 'GET', undefined, value));
      return this.state();
    });
  }
  register(input: SpaceRegistration) {
    return this.authenticate(input, true);
  }
  login(input: SpaceCredentials) {
    return this.authenticate(input, false);
  }
  private authenticate(input: SpaceCredentials, register: boolean) {
    return this.exclusive(async () => {
      if (await this.current())
        throw new UserFacingError('Сначала выйдите из текущего аккаунта Space.');
      const session = readSpaceAccountSession(
        await this.request('/v1/accounts/' + (register ? 'register' : 'login'), 'POST', input),
      );
      try {
        await this.vault.save(session);
      } catch (error) {
        await this.http
          .request('/v1/accounts/logout', 'POST', undefined, session.sessionToken)
          .catch(() => {});
        throw error;
      }
      this.session = session;
      this.previews = { skin: null, cape: null };
      await this.updateProfile({ profile: session.profile });
      return this.state('Вход Space выполнен. Локальный ник сохранён; примените профиль отдельно.');
    });
  }
  logout() {
    return this.exclusive(async () => {
      let remotePending = false;
      try {
        const session = await this.current();
        if (session) await this.request('/v1/accounts/logout', 'POST', undefined, session);
      } catch (error) {
        remotePending = !(error instanceof AccountHttpError && error.code === 'SPACE_UNAUTHORIZED');
      }
      await this.vault.clear();
      this.session = null;
      this.previews = { skin: null, cape: null };
      return this.state(
        remotePending
          ? 'Локальный выход выполнен. API недоступен: серверную сессию отозвать не удалось, она истечёт по сроку. Ник и сохранённый внешний вид оставлены.'
          : 'Вы вышли из Space. Локальный ник и последний применённый внешний вид сохранены.',
      );
    });
  }
  private async savedAppearance(root: string) {
    const bytes = await readBounded(await installationTarget(root, APPEARANCE), 8192);
    if (!bytes) return null;
    const text = bytes.toString('utf8'),
      lines = text.trimEnd().split('\n');
    if (lines.some((line) => line.split('=').length !== 2))
      throw new UserFacingError('Неверный формат сохранённого внешнего вида.');
    const values = Object.fromEntries(lines.map((line) => line.split('=')));
    if (
      lines.length !== 7 ||
      Object.keys(values).sort().join() !==
        'accountId,capeSha256,nickname,schemaVersion,skinModel,skinSha256,username' ||
      values.schemaVersion !== '1' ||
      !/^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}$/.test(values.accountId ?? '') ||
      !/^[A-Za-z0-9_]{3,24}$/.test(values.username ?? '') ||
      !/^[A-Za-z0-9_]{3,16}$/.test(values.nickname ?? '') ||
      !['classic', 'slim'].includes(values.skinModel ?? '')
    )
      throw new UserFacingError(
        'Сохранённый внешний вид не прошёл проверку. Примените профиль Space заново.',
      );
    for (const kind of ['skin', 'cape'] as const) {
      const hash = values[kind + 'Sha256'];
      if (hash === '') continue;
      if (!hash || !/^[a-f0-9]{64}$/.test(hash))
        throw new UserFacingError('Неверный хеш сохранённого внешнего вида.');
      const png = await readBounded(
        await installationTarget(root, `config/space-appearance/${kind}-${hash}.png`),
        256 * 1024,
      );
      if (!png || validatePng(png, kind, values.skinModel).sha256 !== hash)
        throw new UserFacingError('Сохранённый PNG повреждён. Примените профиль Space заново.');
    }
    return values;
  }
  edit(input: SpaceProfileEdit) {
    return this.exclusive(async () => {
      const session = await this.required();
      await this.updateProfile(await this.request('/v1/accounts/me', 'PATCH', input, session));
      return this.state(
        'Ник Space сохранён. Примените его к игре отдельно; offline identity может измениться.',
      );
    });
  }
  upload(kind: 'skin' | 'cape', input: ImageSelection) {
    return this.exclusive(async () => {
      const session = await this.required();
      const path = await this.dependencies.choosePng(kind);
      if (!path) return this.state('Выбор PNG отменён.');
      const bytes = await readBounded(path, 256 * 1024);
      if (!bytes) throw new UserFacingError('Выбранный PNG недоступен.');
      validatePng(bytes, kind, input.model);
      await this.updateProfile(
        await this.request(
          '/v1/accounts/me/' + kind,
          'PUT',
          {
            pngBase64: bytes.toString('base64'),
            expectedRevision: input.expectedRevision,
            ...(kind === 'skin' ? { model: input.model } : {}),
          },
          session,
        ),
      );
      return this.state(
        'Изображение сохранено в Space. Примените профиль перед следующим запуском.',
      );
    });
  }
  remove(kind: 'skin' | 'cape', expectedRevision: number) {
    return this.exclusive(async () => {
      const session = await this.required();
      await this.updateProfile(
        await this.request('/v1/accounts/me/' + kind, 'DELETE', { expectedRevision }, session),
      );
      return this.state('Изображение удалено из Space. Примените профиль к игре отдельно.');
    });
  }
  private async export(root: string, session: SpaceAccountSession) {
    const profile = session.profile;
    const skin = await this.asset(profile.skin, 'skin', profile.skin?.model),
      cape = await this.asset(profile.cape, 'cape');
    if (skin && profile.skin)
      await atomicFile(root, `config/space-appearance/skin-${profile.skin.sha256}.png`, skin);
    if (cape && profile.cape)
      await atomicFile(root, `config/space-appearance/cape-${profile.cape.sha256}.png`, cape);
    const properties = `schemaVersion=1\naccountId=${profile.id}\nusername=${profile.username}\nnickname=${profile.nickname}\nskinSha256=${profile.skin?.sha256 ?? ''}\nskinModel=${profile.skin?.model ?? 'classic'}\ncapeSha256=${profile.cape?.sha256 ?? ''}\n`;
    await atomicFile(root, APPEARANCE, Buffer.from(properties, 'utf8'));
    this.previews = { skin, cape };
  }
  apply(id: string) {
    return this.exclusive(async () => {
      const session = await this.required();
      await this.updateProfile(await this.request('/v1/accounts/me', 'GET', undefined, session));
      const current = await this.required(),
        root = await this.dependencies.rootForProfile(id),
        old = this.dependencies.localNickname();
      await fileTransaction(root, [APPEARANCE], 8192, async () => {
        await this.export(root, current);
        try {
          await this.dependencies.saveNickname(current.profile.nickname);
        } catch (error) {
          if (old) await this.dependencies.saveNickname(old);
          throw error;
        }
      });
      return this.state(
        'Профиль применён. Новый ник может означать другую offline identity и инвентарь.',
      );
    });
  }
  /** Runs inside LaunchService's reservation, before its existing full preflight/spawn. */
  async beforeLaunch(id: string, root: string, name: string, signal: AbortSignal) {
    signal.throwIfAborted();
    await this.exclusive(async () => {
      const saved = await this.savedAppearance(root);
      if (!saved) return; // Guest launch needs no account, encryption provider or network.
      let session: SpaceAccountSession | null;
      try {
        session = await this.current();
      } catch {
        return;
      } // The public snapshot was independently checked above.
      if (!session) return;
      if (saved.accountId !== session.profile.id || saved.nickname !== name) return;
      try {
        await this.updateProfile(await this.request('/v1/accounts/me', 'GET', undefined, session));
      } catch (error) {
        if (
          error instanceof AccountHttpError &&
          ['SPACE_UNAVAILABLE', 'SPACE_UNAUTHORIZED', 'SPACE_RATE_LIMITED'].includes(error.code)
        ) {
          signal.throwIfAborted();
          return;
        }
        throw error;
      }
      signal.throwIfAborted();
      const current = await this.required();
      if (name !== current.profile.nickname)
        throw new UserFacingError(
          'Ник Space изменился. Явно примените профиль либо верните локальный ник перед запуском.',
        );
      await this.export(root, current);
      signal.throwIfAborted();
    });
  }
}
