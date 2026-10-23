import 'reflect-metadata';
import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import type { AppOptions } from './app.module';
import { ApiErrors } from './api-errors';
export async function createApp(options: AppOptions = {}) {
  const app = await NestFactory.create(AppModule.register(options), {
    logger: false,
    abortOnError: false,
  });
  app.getHttpAdapter().getInstance().disable('x-powered-by');
  app.useGlobalFilters(new ApiErrors());
  return app;
}
