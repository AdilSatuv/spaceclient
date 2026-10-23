export const gb = (mb: number) =>
  new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 2 }).format(mb / 1024) + ' ГБ';
