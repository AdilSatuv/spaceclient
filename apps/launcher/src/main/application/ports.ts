import type { Profile, Snapshot, JavaInfo } from '../../shared/models.ts';
import type { InstallationStatus } from '../../shared/installation.ts';
import type { LaunchStatus } from '../../shared/launch.ts';
import type {
  AccountState,
  SpaceCredentials,
  SpaceRegistration,
  SpaceProfileEdit,
  ImageSelection,
  RecoveryResult,
} from '../../shared/account.ts';

export interface ProfileRepository {
  snapshot(): Snapshot;
  profile(id: unknown): Profile;
  save(edit: unknown): Promise<Snapshot>;
  saveNickname(input: unknown): Promise<Snapshot>;
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
  choosePng?(kind: 'skin' | 'cape'): Promise<string | null>;
  exportReport?(bytes: Uint8Array): Promise<boolean>;
}
export interface AccountPort {
  readonly running: boolean;
  status(): Promise<AccountState>;
  register(input: SpaceRegistration): Promise<AccountState>;
  login(input: SpaceCredentials): Promise<AccountState>;
  logout(): Promise<AccountState>;
  edit(input: SpaceProfileEdit): Promise<AccountState>;
  upload(kind: 'skin' | 'cape', input: ImageSelection): Promise<AccountState>;
  remove(kind: 'skin' | 'cape', revision: number): Promise<AccountState>;
  apply(id: string): Promise<AccountState>;
  settled(): Promise<void>;
}
export interface RecoveryPort {
  backup(id: string): Promise<RecoveryResult>;
  restore(id: string): Promise<RecoveryResult>;
  diagnostics(id: string): Promise<RecoveryResult>;
}
export interface LaunchPort {
  readonly running: boolean;
  status(id: string): LaunchStatus;
  start(profile: Profile, nickname: string | null): LaunchStatus;
  shutdown(): Promise<void>;
}
export interface LauncherDependencies {
  account?: AccountPort;
  recovery?: RecoveryPort;
  profiles: ProfileRepository;
  installation: InstallationPort;
  launch: LaunchPort;
  desktop: DesktopPort;
  inspectJava(path: string | null, signal?: AbortSignal): Promise<JavaInfo>;
}
