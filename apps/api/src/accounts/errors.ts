import { HttpException } from '@nestjs/common';
export function accountError(status: number, code: string, message: string): never {
  throw new HttpException({ code, message }, status);
}
