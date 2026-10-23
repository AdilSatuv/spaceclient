import { Module } from '@nestjs/common';
import type { DynamicModule } from '@nestjs/common';
import { ContentModule } from './content/content.module';
import { features, releases, updates } from './content/infrastructure/curated-catalog';
import { ProjectController } from './project.controller';

export type AppOptions = { catalog?: unknown };
@Module({})
export class AppModule {
  static register(options: AppOptions = {}): DynamicModule {
    return {
      module: AppModule,
      imports: [
        ContentModule.register(
          options.catalog === undefined ? { features, releases, updates } : options.catalog,
        ),
      ],
      controllers: [ProjectController],
    };
  }
}
