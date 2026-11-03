import type { SpaceBuildIdentity } from '../shared/build-identity.ts';

/** Explicit allowlist: never spread a manifest/receipt, paths or arbitrary metadata into IPC. */
export function spaceBuildIdentity(build: SpaceBuildIdentity): SpaceBuildIdentity {
  return {
    channel: build.channel,
    modVersion: build.modVersion,
    minecraftVersion: build.minecraftVersion,
    loaderVersion: build.loaderVersion,
    gameSourceTree: build.gameSourceTree,
    sha256: build.sha256,
    size: build.size,
  };
}
