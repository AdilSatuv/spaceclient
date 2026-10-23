import { Module } from '@nestjs/common';
import type { DynamicModule } from '@nestjs/common';
import { CONTENT_REPOSITORY } from './application/catalog-repository';
import type { ContentRepository } from './application/catalog-repository';
import { CatalogService } from './application/catalog-service';
import { StaticContentRepository } from './infrastructure/static-content-repository';
import { ContentController } from './http/content.controller';

@Module({})
export class ContentModule {
  static register(catalog: unknown): DynamicModule {
    return {
      module: ContentModule,
      controllers: [ContentController],
      providers: [
        { provide: CONTENT_REPOSITORY, useFactory: () => new StaticContentRepository(catalog) },
        {
          provide: CatalogService,
          inject: [CONTENT_REPOSITORY],
          useFactory: (repository: ContentRepository) => new CatalogService(repository),
        },
      ],
    };
  }
}
