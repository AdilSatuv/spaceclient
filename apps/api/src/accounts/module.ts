import { Module } from '@nestjs/common';
import { AccountsController } from './controller';
import { AccountService } from './service';
import type { AccountOptions } from './service';
@Module({})
export class AccountsModule {
  static register(options: AccountOptions = {}) {
    return {
      module: AccountsModule,
      controllers: [AccountsController],
      providers: [{ provide: AccountService, useFactory: () => new AccountService(options) }],
    };
  }
}
