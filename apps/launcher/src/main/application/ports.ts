import type { Profile, Snapshot, JavaInfo } from '../../shared/models.ts';
import type { InstallationStatus } from '../../shared/installation.ts';

export interface ProfileRepository {
  snapshot(): Snapshot;
  profile(id: unknown): Profile;
  save(edit: unknown): Promise<Snapshot>;
  create(): Promise<Snapshot>;
  select(id: unknown): Promise<Snapshot>;
  setJava(id: unknown, path: unknown): Promise<Snapshot>;
  instanceDirectory(id: unknown): Promise<string>;
}
export interface InstallationPort {
  readonly running: boolean;
  status(id: string): InstallationStatus;
  start(id: string): InstallationStatus;
  cancel(id: string): Promise<InstallationStatus>;
  shutdown(): Promise<void>;
}
export interface DesktopPort {
  chooseJava(): Promise<string | null>;
  openDirectory(path: string): Promise<void>;
}
export interface LauncherDependencies {
  profiles: ProfileRepository;
  installation: InstallationPort;
  desktop: DesktopPort;
  inspectJava(path: string | null): Promise<JavaInfo>;
}
