import type { JavaInfo, ProfileEdit, Result, Snapshot } from './models.ts';
import type { InstallationStatus } from './installation.ts';
import type { LaunchStatus } from './launch.ts';
import type {
  AccountState,
  SpaceCredentials,
  SpaceRegistration,
  SpaceProfileEdit,
  ImageSelection,
  RecoveryResult,
} from './account.ts';

export type Commands = {
  'account:get': { input: void; output: AccountState };
  'account:register': { input: SpaceRegistration; output: AccountState };
  'account:login': { input: SpaceCredentials; output: AccountState };
  'account:logout': { input: void; output: AccountState };
  'account:edit': { input: SpaceProfileEdit; output: AccountState };
  'account:skin:upload': { input: ImageSelection; output: AccountState };
  'account:cape:upload': { input: ImageSelection; output: AccountState };
  'account:skin:delete': { input: number; output: AccountState };
  'account:cape:delete': { input: number; output: AccountState };
  'account:apply': {
    input: { id: string; confirmIdentityChange: true };
    output: { account: AccountState; snapshot: Snapshot };
  };
  'profile:backup': { input: string; output: RecoveryResult };
  'profile:restore': {
    input: { id: string; confirmed: true };
    output: { result: RecoveryResult; snapshot: Snapshot };
  };
  'diagnostics:export': { input: string; output: RecoveryResult };
  'local:save': { input: string; output: Snapshot };
  'launch:get': { input: string; output: LaunchStatus };
  'launch:start': { input: string; output: LaunchStatus };
  'state:get': { input: void; output: Snapshot };
  'profile:save': { input: ProfileEdit; output: Snapshot };
  'profile:create': { input: void; output: Snapshot };
  'profile:select': { input: string; output: Snapshot };
  'java:choose': { input: string; output: Snapshot };
  'java:check': { input: string; output: JavaInfo };
  'profile:open': { input: string; output: null };
  'installation:get': { input: string; output: InstallationStatus };
  'installation:start': { input: string; output: InstallationStatus };
  'installation:cancel': { input: string; output: InstallationStatus };
};
export const channels = {
  getAccount: 'account:get',
  registerAccount: 'account:register',
  loginAccount: 'account:login',
  logoutAccount: 'account:logout',
  editAccount: 'account:edit',
  uploadSkin: 'account:skin:upload',
  uploadCape: 'account:cape:upload',
  deleteSkin: 'account:skin:delete',
  deleteCape: 'account:cape:delete',
  applyAccount: 'account:apply',
  backupProfile: 'profile:backup',
  restoreProfile: 'profile:restore',
  exportDiagnostics: 'diagnostics:export',
  saveNickname: 'local:save',
  getLaunch: 'launch:get',
  startLaunch: 'launch:start',
  getState: 'state:get',
  saveProfile: 'profile:save',
  createProfile: 'profile:create',
  selectProfile: 'profile:select',
  chooseJava: 'java:choose',
  checkJava: 'java:check',
  openInstance: 'profile:open',
  getInstallation: 'installation:get',
  startInstallation: 'installation:start',
  cancelInstallation: 'installation:cancel',
} as const satisfies Record<string, keyof Commands>;

export type CommandName = keyof Commands;
export type CommandOutput<K extends CommandName> = Commands[K]['output'];
export type CommandArgs<K extends CommandName> = Commands[K]['input'] extends void
  ? []
  : [input: Commands[K]['input']];
// Runtime payload is untrusted even though callers have a typed API.
export type CommandHandler<K extends CommandName> = (
  payload: unknown,
) => CommandOutput<K> | Promise<CommandOutput<K>>;
export type CommandHandlers = { [K in CommandName]: CommandHandler<K> };
export type LauncherApi = {
  [M in keyof typeof channels]: (
    ...args: CommandArgs<(typeof channels)[M]>
  ) => Promise<Result<CommandOutput<(typeof channels)[M]>>>;
};
