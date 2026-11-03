import {
  Body,
  Controller,
  Delete,
  Get,
  Headers,
  Inject,
  Param,
  Patch,
  Post,
  Put,
  Req,
  Res,
} from '@nestjs/common';
import { AccountService } from './service';
import { accountError } from './errors';
import { fields } from './input';

@Controller('v1/accounts')
export class AccountsController {
  constructor(@Inject(AccountService) private readonly accounts: AccountService) {}
  private token(header: unknown) {
    return typeof header === 'string' && header.startsWith('Bearer ') ? header.slice(7) : undefined;
  }
  private peer(req: any) {
    return req.socket.remoteAddress ?? 'unknown';
  }
  private mutate(req: any, header: unknown) {
    this.accounts.spend(`mutation:${this.peer(req)}:${this.token(header) ?? ''}`, 60, 60000);
  }
  @Post('register') register(@Body() body: unknown, @Req() req: any) {
    return this.accounts.register(body, this.peer(req));
  }
  @Post('login') login(@Body() body: unknown, @Req() req: any) {
    return this.accounts.login(body, this.peer(req));
  }
  @Post('logout') logout(
    @Headers('authorization') header: unknown,
    @Req() req: any,
    @Body() body: unknown,
  ) {
    fields(body ?? {}, []);
    this.mutate(req, header);
    return this.accounts.logout(this.token(header));
  }
  @Get('me') me(@Headers('authorization') header: unknown) {
    return this.accounts.me(this.token(header));
  }
  @Patch('me') rename(
    @Headers('authorization') header: unknown,
    @Body() body: unknown,
    @Req() req: any,
  ) {
    this.mutate(req, header);
    return this.accounts.rename(this.token(header), body);
  }
  @Put('me/skin') skin(
    @Headers('authorization') header: unknown,
    @Body() body: unknown,
    @Req() req: any,
  ) {
    this.mutate(req, header);
    return this.accounts.upload(this.token(header), body, 'skin');
  }
  @Put('me/cape') cape(
    @Headers('authorization') header: unknown,
    @Body() body: unknown,
    @Req() req: any,
  ) {
    this.mutate(req, header);
    return this.accounts.upload(this.token(header), body, 'cape');
  }
  @Delete('me/skin') deleteSkin(
    @Headers('authorization') header: unknown,
    @Body() body: unknown,
    @Req() req: any,
  ) {
    this.mutate(req, header);
    return this.accounts.remove(this.token(header), body, 'skin');
  }
  @Delete('me/cape') deleteCape(
    @Headers('authorization') header: unknown,
    @Body() body: unknown,
    @Req() req: any,
  ) {
    this.mutate(req, header);
    return this.accounts.remove(this.token(header), body, 'cape');
  }
  @Get('assets/:sha256') asset(@Param('sha256') hash: string, @Req() req: any, @Res() res: any) {
    this.accounts.spend(`assets:${this.peer(req)}`, 240, 60000);
    const bytes = this.accounts.asset(hash);
    res.set({
      'Content-Type': 'image/png',
      'X-Content-Type-Options': 'nosniff',
      'Cache-Control': 'public, max-age=31536000, immutable',
      'Content-Security-Policy': "default-src 'none'; sandbox",
    });
    res.send(bytes);
  }
}
