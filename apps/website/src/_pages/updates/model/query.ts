import { validUpdatePage, validUpdateType } from '@space/content-contracts';
import type { UpdateQuery } from '@space/content-contracts';
export type SearchParams = Record<string, string | string[] | undefined>;

// Browser URLs are forgiving; the HTTP API deliberately rejects malformed queries.
export function updatesQuery(params: SearchParams): UpdateQuery {
  return {
    type: validUpdateType(params.type) ? params.type : 'all',
    page: validUpdatePage(params.page) ? Number(params.page) : 1,
  };
}
