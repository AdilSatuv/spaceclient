export type InstallationPhase =
  | 'idle'
  | 'planning'
  | 'downloading'
  | 'extracting'
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
  message: string;
};
export const installationRunning = (phase: InstallationPhase) =>
  ['planning', 'downloading', 'extracting', 'cancelling'].includes(phase);
