import { readFile } from 'node:fs/promises';
import { join } from 'node:path';
import { CSP, assetName } from '../security.ts';

export function createAssetHandler(rendererRoot: string) {
  return async (request: Request): Promise<Response> => {
    const name = request.method === 'GET' ? assetName(request.url) : null;
    if (!name) return new Response('Not found', { status: 404 });
    const type = name.endsWith('.js')
      ? 'text/javascript'
      : name.endsWith('.css')
        ? 'text/css'
        : name.endsWith('.png')
          ? 'image/png'
          : 'text/html';
    try {
      return new Response(await readFile(join(rendererRoot, name)), {
        headers: {
          'Content-Type': type,
          'Content-Security-Policy': CSP,
          'X-Content-Type-Options': 'nosniff',
        },
      });
    } catch {
      return new Response('Not found', { status: 404 });
    }
  };
}
