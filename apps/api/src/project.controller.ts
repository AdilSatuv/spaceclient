import { Controller, Get } from '@nestjs/common';
import type { ProjectStatus } from '@space/content-contracts';

@Controller('v1')
export class ProjectController {
  @Get('health') health() {
    return { status: 'ok', service: 'space-api' };
  }
  @Get('project') project(): ProjectStatus {
    return {
      name: 'Space Client',
      stage: 'development',
      minecraft: '1.8.9',
      platforms: ['windows-x64'],
      repository: 'https://github.com/AdilSatuv/spaceclient',
    };
  }
}
