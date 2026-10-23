import { getProject } from '@/shared/api/index.server';
export default async function ProjectStatus() {
  const result = await getProject();
  const label = result.ok
    ? 'API подключён · Space Client в разработке'
    : 'API временно недоступен. Прогресс — в GitHub.';
  return <p className="api-status wrap">{label}</p>;
}
