export type InstallationPhase =
  | 'idle'
  | 'planning'
  | 'downloading'
  | 'extracting'
  | 'installing'
  | 'cancelling'
  | 'prepared'
  | 'cancelled'
  | 'error';
export type InstallationStatus = {
  profileId: string;
  phase: InstallationPhase;
  completedFiles: number;
  totalFiles: number;
  receivedBytes: number;
  verifiedBytes: number;
  totalBytes: number;
  retryCount: number;
  spaceVersion: string | null;
  message: string;
};
export const installationRunning = (phase: InstallationPhase) =>
  ['planning', 'downloading', 'extracting', 'installing', 'cancelling'].includes(phase);
