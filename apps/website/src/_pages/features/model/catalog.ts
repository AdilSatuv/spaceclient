import type { Feature } from '@space/content-contracts';

export const categoryLabels: Record<string, string> = {
  hud: 'HUD-модули',
  game: 'Игровые модули',
  tools: 'Инструменты',
  comfort: 'Управление и оформление',
  launcher: 'Лаунчер',
  performance: 'Производительность',
};

export function featureKind(item: Feature) {
  if (item.category === 'hud') return 'hud';
  if (item.id.startsWith('game-')) return 'game';
  if (item.id === 'editor' || item.id === 'profiles') return 'tools';
  return 'other';
}

export const kindLabels = {
  hud: 'HUD-модуль',
  game: 'Игровой модуль',
  tools: 'Инструмент',
  other: 'Возможность',
};

export function matchesCategory(item: Feature, category: string) {
  if (!category) return true;
  if (category === 'game' || category === 'tools') return featureKind(item) === category;
  return item.category === category;
}

export function filterFeatures(items: Feature[], q: string, category: string, status: string) {
  const query = q.toLocaleLowerCase('ru').replace(/ё/g, 'е');
  return items.filter(
    (item) =>
      matchesCategory(item, category) &&
      (!status || status === item.status) &&
      `${item.id} ${item.name} ${item.summary} ${item.details}`
        .toLocaleLowerCase('ru')
        .replace(/ё/g, 'е')
        .includes(query),
  );
}
