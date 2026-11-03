import { ArgumentsHost, Catch, ExceptionFilter, HttpException } from '@nestjs/common';

@Catch()
export class ApiErrors implements ExceptionFilter {
  catch(exception: unknown, host: ArgumentsHost) {
    const response = host.switchToHttp().getResponse();
    const request = host.switchToHttp().getRequest();
    const parserType = (exception as { type?: string } | null)?.type;
    const status =
      exception instanceof HttpException
        ? exception.getStatus()
        : parserType === 'entity.too.large'
          ? 413
          : parserType === 'entity.parse.failed'
            ? 400
            : 500;
    const detail = exception instanceof HttpException ? exception.getResponse() : undefined;
    const record =
      typeof detail === 'object' && detail !== null ? (detail as Record<string, unknown>) : {};
    const account = String(request.url).startsWith('/v1/accounts');
    const code =
      typeof record.code === 'string'
        ? record.code
        : account
          ? status === 500
            ? 'SPACE_UNAVAILABLE'
            : 'SPACE_INVALID_INPUT'
          : status === 404
            ? 'NOT_FOUND'
            : status === 400
              ? 'BAD_REQUEST'
              : 'INTERNAL_ERROR';
    const message =
      typeof record.code === 'string' && typeof record.message === 'string'
        ? record.message
        : account
          ? status === 500
            ? 'Сервис аккаунтов временно недоступен.'
            : 'Некорректный запрос аккаунта.'
          : status === 404
            ? 'Ресурс не найден'
            : status === 400
              ? 'Некорректный запрос'
              : 'Внутренняя ошибка сервиса';
    response.status(status).json({ statusCode: status, code, message });
  }
}
