import { BadRequestException, Controller, Get, Module, NotFoundException, Param, Query } from '@nestjs/common';
import { features, releases, updates, validSlug, validateContent } from './content';

function queryString(value: unknown, name: string, allowed?: string[]): string | undefined {
  if (value === undefined) return undefined;
  if (typeof value !== 'string' || value.length > 100 || (allowed && !allowed.includes(value))) throw new BadRequestException({ statusCode: 400, code: 'INVALID_QUERY', message: `Некорректный параметр ${name}` });
  return value;
}

@Controller('v1')
class ProjectController {
  constructor() { validateContent({ features, releases, updates }); }
  @Get('health') health() { return { status: 'ok', service: 'space-api' }; }
  @Get('project') project() {
    return { name: 'Space Client', stage: 'development', minecraft: '1.8.9', platforms: ['windows-x64'], repository: 'https://github.com/AdilSatuv/spaceclient' };
  }
  @Get('releases') releases() {
    const items = [...releases].sort((a,b) => b.publishedAt.localeCompare(a.publishedAt) || a.id.localeCompare(b.id));
    return { items, total: items.length };
  }
  @Get('features') features() { return { items: features }; }
  @Get('updates') updates(@Query() query: Record<string, unknown>) {
    if (Object.keys(query).some(key => !['type','page'].includes(key))) throw new BadRequestException({ statusCode: 400, code: 'INVALID_QUERY', message: 'Неизвестный параметр' });
    const type = queryString(query.type, 'type', ['all','development','release']);
    const pageString = queryString(query.page, 'page');
    if (pageString !== undefined && !/^[1-9]\d{0,5}$/.test(pageString)) throw new BadRequestException({ statusCode: 400, code: 'INVALID_QUERY', message: 'Некорректная страница' });
    const page = Number(pageString || 1); const pageSize = 12;
    const items = updates.filter(item => !type || type === 'all' || item.type === type).sort((a,b) => b.publishedAt.localeCompare(a.publishedAt) || a.id.localeCompare(b.id));
    return { items: items.slice((page-1)*pageSize,page*pageSize), page, pageSize, total: items.length };
  }
  @Get('updates/:slug') update(@Param('slug') slug: string) {
    if (!validSlug(slug)) throw new BadRequestException({ statusCode: 400, code: 'INVALID_SLUG', message: 'Некорректный адрес записи' });
    const item = updates.find(item => item.slug === slug);
    if (!item) throw new NotFoundException({ statusCode: 404, code: 'UPDATE_NOT_FOUND', message: 'Запись не найдена' });
    return item;
  }
}
@Module({ controllers: [ProjectController] })
export class AppModule {}
