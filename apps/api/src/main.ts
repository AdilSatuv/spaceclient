import { createApp } from './app';
async function main() {
  const app = await createApp();
  const port = Number(process.env.PORT || 3001);
  if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error('Invalid PORT');
  await app.listen(port, '127.0.0.1');
  console.log(`Space API: http://127.0.0.1:${port}/v1/health`);
}
main().catch(error => { console.error(error); process.exitCode = 1; });
