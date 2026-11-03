import { createHash } from 'node:crypto';
import type { ProfileRepository } from './application/ports.ts';
import type { InstallationStatus } from '../shared/installation.ts';
import type { LaunchStatus } from '../shared/launch.ts';
import type { SpaceBuildIdentity } from '../shared/build-identity.ts';
import { atomicFile, fileTransaction, readBounded } from './bounded-files.ts';
import { installationTarget } from './downloads.ts';
import { UserFacingError } from './application/user-facing-error.ts';
import { spaceBuildIdentity } from './build-identity.ts';

export const RECOVERY_FILES = [
  'options.txt',
  'config/spaceclient-hud.properties',
  'config/spaceclient-mods.properties',
  'config/spaceclient-browser.properties',
  'config/spaceclient-waypoints.properties',
  'config/spaceclient-cosmetics.properties',
] as const;
const digest = (value: unknown) => createHash('sha256').update(JSON.stringify(value)).digest('hex');
type Backup = {
  schemaVersion: 1;
  profileId: string;
  createdAt: string;
  profile: { name: string; memoryMb: number };
  files: Record<string, string | null>;
};
type Dependencies = {
  profiles: ProfileRepository;
  installation(id: string): InstallationStatus;
  launch(id: string): LaunchStatus;
  build: SpaceBuildIdentity | null;
  exportReport(bytes: Buffer): Promise<boolean>;
};
export class ProfileRecovery {
  private readonly root: string;
  private readonly dependencies: Dependencies;
  private readonly writer: typeof atomicFile;
  constructor(root: string, dependencies: Dependencies, writer = atomicFile) {
    this.root = root;
    this.dependencies = dependencies;
    this.writer = writer;
  }
  private async instance(id: string) {
    return (await this.dependencies.profiles.instanceDirectory(id)) + '/minecraft';
  }
  async backup(id: string) {
    const profile = this.dependencies.profiles.profile(id),
      root = await this.instance(id),
      files: Record<string, string | null> = {};
    for (const path of RECOVERY_FILES)
      files[path] =
        (await readBounded(await installationTarget(root, path), 512 * 1024))?.toString('base64') ??
        null;
    const content: Backup = {
      schemaVersion: 1,
      profileId: id,
      createdAt: new Date().toISOString(),
      profile: { name: profile.name, memoryMb: profile.memoryMb },
      files,
    };
    const bytes = Buffer.from(JSON.stringify({ content, sha256: digest(content) }));
    if (bytes.length > 4 * 1024 * 1024)
      throw new UserFacingError('Настройки превышают лимит резервной копии.');
    await atomicFile(this.root, `recovery/${id}.json`, bytes);
    return {
      message: 'Резервная копия настроек этого профиля сохранена. Миры и аккаунты не включены.',
      files: RECOVERY_FILES.length,
    };
  }
  async restore(id: string) {
    const current = this.dependencies.profiles.profile(id),
      bytes = await readBounded(
        await installationTarget(this.root, `recovery/${id}.json`),
        4 * 1024 * 1024,
      );
    let value: Backup;
    try {
      if (!bytes) throw 0;
      const envelope = JSON.parse(bytes.toString('utf8'));
      value = envelope.content;
      if (
        Object.keys(envelope).sort().join() !== 'content,sha256' ||
        envelope.sha256 !== digest(value) ||
        !value ||
        Object.keys(value).sort().join() !== 'createdAt,files,profile,profileId,schemaVersion' ||
        value.schemaVersion !== 1 ||
        value.profileId !== id ||
        !Number.isFinite(Date.parse(value.createdAt)) ||
        Object.keys(value.profile).sort().join() !== 'memoryMb,name' ||
        Object.keys(value.files).sort().join() !== [...RECOVERY_FILES].sort().join()
      )
        throw 0;
      for (const text of Object.values(value.files))
        if (
          text !== null &&
          (typeof text !== 'string' ||
            text.length > 700 * 1024 ||
            Buffer.from(text, 'base64').toString('base64') !== text ||
            Buffer.from(text, 'base64').length > 512 * 1024)
        )
          throw 0;
      if (
        typeof value.profile.name !== 'string' ||
        !value.profile.name.trim() ||
        value.profile.name.length > 40 ||
        /[\x00-\x1f\x7f]/.test(value.profile.name) ||
        !Number.isInteger(value.profile.memoryMb) ||
        value.profile.memoryMb < 1024 ||
        value.profile.memoryMb > this.dependencies.profiles.snapshot().maxMemoryMb ||
        value.profile.memoryMb % 256
      )
        throw 0;
    } catch {
      throw new UserFacingError(
        'Резервная копия отсутствует, повреждена или относится к другому профилю.',
      );
    }
    const root = await this.instance(id);
    await fileTransaction(root, RECOVERY_FILES, 512 * 1024, async () => {
      for (const path of RECOVERY_FILES)
        await this.writer(
          root,
          path,
          value.files[path] === null ? null : Buffer.from(value.files[path]!, 'base64'),
        );
      await this.dependencies.profiles.save({
        id,
        name: value.profile.name,
        memoryMb: value.profile.memoryMb,
      });
    });
    return {
      message: `Настройки профиля «${current.name}» восстановлены. Миры и текущая сессия Space сохранены.`,
      files: RECOVERY_FILES.length,
    };
  }
  async diagnostics(id: string) {
    const profile = this.dependencies.profiles.profile(id),
      installation = this.dependencies.installation(id),
      launch = this.dependencies.launch(id);
    const report = {
      schemaVersion: 1,
      kind: 'space-safe-diagnostics',
      createdAt: new Date().toISOString(),
      launcherVersion: '0.1.0',
      platform: process.platform,
      build: this.dependencies.build ? spaceBuildIdentity(this.dependencies.build) : null,
      profile: { memoryMb: profile.memoryMb, javaSelected: !!profile.javaPath },
      installation: {
        phase: installation.phase,
        completedFiles: installation.completedFiles,
        totalFiles: installation.totalFiles,
        retryCount: installation.retryCount,
      },
      launch: { phase: launch.phase, exitCode: launch.exitCode },
    };
    const exported = await this.dependencies.exportReport(
      Buffer.from(JSON.stringify(report, null, 2) + '\n'),
    );
    return {
      message: exported
        ? 'Диагностика экспортирована: без ников, путей, токенов, журналов и игровых данных.'
        : 'Экспорт диагностики отменён.',
      files: exported ? 1 : 0,
    };
  }
}
