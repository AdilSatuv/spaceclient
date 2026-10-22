import { Controller, Get, Module } from '@nestjs/common';

@Controller('v1')
class ProjectController {
  @Get('health') health() { return { status: 'ok', service: 'space-api' }; }
  @Get('project') project() {
    return { name: 'Space Client', stage: 'development', minecraft: '1.8.9', platforms: ['windows-x64'], repository: 'https://github.com/AdilSatuv/spaceclient' };
  }
  @Get('releases') releases() {
    return { releases: [], message: 'Пользовательских релизов пока нет.' };
  }
}
@Module({ controllers: [ProjectController] })
export class AppModule {}
