import type { UpdateQuery } from '@space/content-contracts';
import { UPDATE_PAGE_SIZE } from '@space/content-contracts';
import type { ContentRepository } from './catalog-repository';

export class CatalogService {
  constructor(private readonly repository: ContentRepository) {}
  features() {
    return { items: this.repository.read().features };
  }
  releases() {
    const items = this.repository
      .read()
      .releases.sort(
        (a, b) => b.publishedAt.localeCompare(a.publishedAt) || a.id.localeCompare(b.id),
      );
    return { items, total: items.length };
  }
  updates({ type, page }: UpdateQuery) {
    const items = this.repository
      .read()
      .updates.filter((item) => type === 'all' || item.type === type)
      .sort((a, b) => b.publishedAt.localeCompare(a.publishedAt) || a.id.localeCompare(b.id));
    return {
      items: items.slice((page - 1) * UPDATE_PAGE_SIZE, page * UPDATE_PAGE_SIZE),
      page,
      pageSize: UPDATE_PAGE_SIZE,
      total: items.length,
    };
  }
  update(slug: string) {
    return this.repository.read().updates.find((item) => item.slug === slug);
  }
}
