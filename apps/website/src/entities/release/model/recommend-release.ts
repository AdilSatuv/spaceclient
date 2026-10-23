import type { Release } from '@space/content-contracts';
export function recommendedRelease(items: Release[]): Release | undefined {
  const compatible = items
    .filter((x) => !x.withdrawn && x.minecraftVersions.includes('1.8.9'))
    .sort((a, b) => b.publishedAt.localeCompare(a.publishedAt) || a.id.localeCompare(b.id));
  return compatible.find((x) => x.channel === 'stable') || compatible[0];
}
