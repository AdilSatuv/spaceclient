import 'reflect-metadata';
import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';
import type { AppOptions } from './app.module';
import { ApiErrors } from './api-errors';
import type { NestExpressApplication } from '@nestjs/platform-express';
export async function createApp(options: AppOptions = {}) {
  const app = await NestFactory.create<NestExpressApplication>(AppModule.register(options), {
    logger: false,
    abortOnError: false,
    bodyParser: false,
  });
  app.useBodyParser('json', { limit: '360kb', strict: true });
  app.use((req: any, res: any, next: () => void) => {
    if (req.path.startsWith('/v1/accounts')) {
      res.setHeader('Cache-Control', 'no-store');
      res.setHeader('X-Content-Type-Options', 'nosniff');
      if (Object.keys(req.query).length)
        return res.status(400).json({
          statusCode: 400,
          code: 'SPACE_INVALID_INPUT',
          message: 'Некорректные параметры запроса.',
        });
    }
    next();
  });
  app.getHttpAdapter().getInstance().disable('x-powered-by');
  app.useGlobalFilters(new ApiErrors());
  return app;
}
