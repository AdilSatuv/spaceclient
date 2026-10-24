export type LaunchStatus = {
  profileId: string;
  mode: 'local';
  phase: 'idle' | 'checking' | 'starting' | 'running' | 'exited' | 'error';
  message: string;
  exitCode: number | null;
  log: string[];
};
