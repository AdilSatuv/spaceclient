import { createApp } from './app';
async function main() {
  const app = await createApp();
  const port = Number(process.env.PORT || 3001);
  const host = process.env.HOST || '127.0.0.1';
  if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error('Invalid PORT');
  if (!['127.0.0.1', '0.0.0.0'].includes(host)) throw new Error('Invalid HOST');
  app.enableShutdownHooks();
  await app.listen(port, host);
  console.log(`Space API: http://${host}:${port}/v1/health`);
}
main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
