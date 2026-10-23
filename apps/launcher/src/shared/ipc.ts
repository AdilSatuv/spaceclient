import type { JavaInfo, ProfileEdit, Result, Snapshot } from './models.ts';
import type { InstallationStatus } from './installation.ts';

export type Commands = {
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
