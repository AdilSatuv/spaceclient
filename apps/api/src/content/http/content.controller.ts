import {
  BadRequestException,
  Controller,
  Get,
  NotFoundException,
  Param,
  Query,
} from '@nestjs/common';
import { validSlug } from '@space/content-contracts';
import { CatalogService } from '../application/catalog-service';
import { parseUpdateQuery } from './query';

@Controller('v1')
export class ContentController {
  constructor(private readonly catalog: CatalogService) {}
  @Get('releases') releases() {
    return this.catalog.releases();
  }
  @Get('features') features() {
    return this.catalog.features();
  }
  @Get('updates') updates(@Query() query: Record<string, unknown>) {
    return this.catalog.updates(parseUpdateQuery(query));
  }
  @Get('updates/:slug') update(@Param('slug') slug: string) {
    if (!validSlug(slug))
      throw new BadRequestException({
        statusCode: 400,
        code: 'INVALID_SLUG',
        message: 'Некорректный адрес записи',
      });
    const item = this.catalog.update(slug);
    if (!item)
      throw new NotFoundException({
        statusCode: 404,
        code: 'UPDATE_NOT_FOUND',
        message: 'Запись не найдена',
      });
    return item;
  }
}
