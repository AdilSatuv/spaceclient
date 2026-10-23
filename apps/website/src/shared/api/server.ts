import 'server-only';
import { cache } from 'react';
import type { UpdateQuery } from '@space/content-contracts';
import { createContentClient } from './content-client';

// Request-time configuration keeps the same build usable with a private API origin.
// React cache deduplicates metadata/page reads within the render; no global data cache.
const client = () =>
  createContentClient({ origin: process.env.SPACE_API_URL || 'http://127.0.0.1:3001' });
export const getProject = cache(() => client().getProject());
export const getFeatures = cache(() => client().getFeatures());
export const getReleases = cache(() => client().getReleases());
export const getUpdates = cache((type: UpdateQuery['type'], page: number) =>
  client().getUpdates(type, page),
);
export const getUpdate = cache((slug: string) => client().getUpdate(slug));
