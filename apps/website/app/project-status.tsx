export default async function ProjectStatus() {
  let label = 'API временно недоступен. Прогресс — в GitHub.';
  try {
    const origin = process.env.SPACE_API_URL || 'http://127.0.0.1:3001';
    const response = await fetch(`${origin}/v1/project`, { cache: 'no-store', signal: AbortSignal.timeout(2000) });
    if (response.ok) {
      const data: unknown = await response.json();
      if (typeof data === 'object' && data !== null && 'stage' in data && data.stage === 'development') {
        label = 'API подключён · Space Client в разработке';
      }
    }
  } catch { /* The page remains useful when the API is offline. */ }
  return <p className="api-status wrap">{label}</p>;
}
