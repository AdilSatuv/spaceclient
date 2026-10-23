import { BadRequestException } from '@nestjs/common';
import { validUpdatePage, validUpdateType } from '@space/content-contracts';
import type { UpdateQuery } from '@space/content-contracts';

export function parseUpdateQuery(query: Record<string, unknown>): UpdateQuery {
  const { type = 'all', page = '1' } = query;
  if (
    Object.keys(query).some((key) => !['type', 'page'].includes(key)) ||
    !validUpdateType(type) ||
    !validUpdatePage(page)
  ) {
    throw new BadRequestException({
      statusCode: 400,
      code: 'INVALID_QUERY',
      message: 'Некорректные параметры журнала',
    });
  }
  return { type, page: Number(page) };
}
