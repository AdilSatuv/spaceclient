import { validateContent } from '@space/content-contracts';
import type { ContentCatalog } from '@space/content-contracts';
import type { ContentRepository } from '../application/catalog-repository';

export class StaticContentRepository implements ContentRepository {
  private readonly catalog: ContentCatalog;
  constructor(input: unknown) {
    validateContent(input);
    this.catalog = structuredClone(input);
  }
  read(): ContentCatalog {
    return structuredClone(this.catalog);
  }
}
