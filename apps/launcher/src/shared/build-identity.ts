/** Public provenance only. A build ID does not grant permission to launch. */
export type SpaceBuildIdentity = {
  channel: 'development';
  modVersion: '0.1.0';
  minecraftVersion: '1.8.9';
  loaderVersion: '0.18.3';
  gameSourceTree: string;
  sha256: string;
  size: number;
};

export type VerifiedSpaceBuild = {
  identity: SpaceBuildIdentity;
  checkedAt: string;
};

export const developmentBuildId = (build: SpaceBuildIdentity) =>
  `dev-${build.gameSourceTree.slice(0, 12)}`;
