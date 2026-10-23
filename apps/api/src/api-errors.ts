import { ArgumentsHost, Catch, ExceptionFilter, HttpException } from '@nestjs/common';

@Catch()
export class ApiErrors implements ExceptionFilter {
  catch(exception: unknown, host: ArgumentsHost) {
    const response = host.switchToHttp().getResponse();
    const status = exception instanceof HttpException ? exception.getStatus() : 500;
    const detail = exception instanceof HttpException ? exception.getResponse() : undefined;
    const record =
      typeof detail === 'object' && detail !== null ? (detail as Record<string, unknown>) : {};
    const code =
      typeof record.code === 'string'
        ? record.code
        : status === 404
          ? 'NOT_FOUND'
          : status === 400
            ? 'BAD_REQUEST'
            : 'INTERNAL_ERROR';
    const message =
      typeof record.code === 'string' && typeof record.message === 'string'
        ? record.message
        : status === 404
          ? 'Ресурс не найден'
          : status === 400
            ? 'Некорректный запрос'
            : 'Внутренняя ошибка сервиса';
    response.status(status).json({ statusCode: status, code, message });
  }
}
