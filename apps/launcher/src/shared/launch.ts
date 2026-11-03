import type { VerifiedSpaceBuild } from './build-identity.ts';

export type LaunchStatus = {
  profileId: string;
  mode: 'local';
  phase: 'idle' | 'checking' | 'starting' | 'running' | 'exited' | 'error';
  message: string;
  exitCode: number | null;
  log: string[];
  verifiedBuild: VerifiedSpaceBuild | null;
};
