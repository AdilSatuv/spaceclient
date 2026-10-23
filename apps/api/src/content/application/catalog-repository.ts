import type { ContentCatalog } from '@space/content-contracts';

export const CONTENT_REPOSITORY = Symbol('CONTENT_REPOSITORY');
export interface ContentRepository {
  read(): ContentCatalog;
}
