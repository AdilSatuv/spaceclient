import type { Feature, Release, Update } from '@space/content-contracts';
export const features: Feature[] = [
  {
    id: 'fps',
    slug: 'fps',
    name: 'FPS Counter',
    summary: 'Частота кадров прямо в игре.',
    category: 'hud',
    status: 'prototype',
    details:
      'Показатель берётся из Minecraft; отрисовка в тестовом мире проверена. Замеры производительности ещё не проводились.',
  },
  {
    id: 'coordinates',
    slug: 'coordinates',
    name: 'Координаты',
    summary: 'XYZ без отладочного экрана.',
    category: 'hud',
    status: 'prototype',
    details: 'В dev-моде реализован виджет положения игрока.',
  },
  {
    id: 'keys',
    slug: 'keystrokes',
    name: 'Keystrokes',
    summary: 'Состояния клавиш движения перед глазами.',
    category: 'hud',
    status: 'prototype',
    details: 'Использует игровые bindings; полноценный редактор ещё в планах.',
  },
  {
    id: 'cps',
    slug: 'cps',
    name: 'CPS Counter',
    summary: 'Отдельный счётчик левой и правой кнопок.',
    category: 'hud',
    status: 'prototype',
    details:
      'Добавлено скользящее окно в одну секунду и тесты счётчика. Левый клик проверен в мире; полная игровая проверка правой кнопки, удержания и меню ещё впереди.',
  },
  {
    id: 'editor',
    slug: 'hud-editor',
    name: 'Редактор HUD',
    summary: 'Перемещение, масштаб и настройки виджетов.',
    category: 'comfort',
    status: 'planned',
    details: 'Цель — менять расположение модулей из самой игры.',
  },
  {
    id: 'profiles',
    slug: 'launch-profiles',
    name: 'Профили запуска',
    summary: 'Свои настройки памяти и игры.',
    category: 'launcher',
    status: 'verified',
    details:
      'В Windows-прототипе проверены независимые профили, сохранение памяти и Java после перезапуска. Подготовка игровых файлов также работает. Публичного установщика, рабочего входа и запуска игры из лаунчера пока нет.',
  },
];
export const releases: Release[] = [];
export const updates: Update[] = [
  {
    id: 'launcher-and-website',
    slug: 'launcher-and-website',
    type: 'development',
    title: 'Лаунчер уже готовит файлы. Сайт получил новую основу.',
    summary: 'Профили, Java и подготовка development-инстанса проверены; сайт переведён на FSD.',
    publishedAt: '2026-10-24T00:00:00.000Z',
    blocks: [
      {
        heading: 'Что работает в прототипе',
        text: 'Лаунчер сохраняет отдельные профили, память и выбранную Java. Проверены подготовка и восстановление Minecraft 1.8.9, загрузчика, natives и собственного development JAR Space. Файлы профиля хранятся отдельно от существующей .minecraft.',
      },
      {
        heading: 'Сайт и интерфейс',
        text: 'Лаунчер получил интерфейс на shadcn/ui. Вебсайт сохраняет космический стиль, а его код разделён по FSD. Next.js и NestJS используют общий контракт контента; ошибки API отличаются от пустого каталога.',
      },
      {
        heading: 'Что ещё впереди',
        text: 'Регистрация Space и вход по нику выбраны для следующего этапа, но пока не реализованы. Запуска игры из лаунчера и публичного установщика нет. Подготовка development-файлов не означает готовый пользовательский релиз.',
      },
    ],
  },
  {
    id: 'first-contact',
    slug: 'first-contact',
    type: 'development',
    title: 'Начинаем с фундамента.',
    summary: 'Первый Java-мод, исправление логирования и новый дизайн сайта в Figma.',
    publishedAt: '2026-10-23T00:00:00.000Z',
    blocks: [
      {
        heading: 'Первый игровой прототип',
        text: 'Подготовлена сборка Minecraft 1.8.9 на Legacy Fabric. В dev-моде есть FPS, XYZ, состояния клавиш и переключение HUD через F8. Проверка внутри мира ещё не завершена.',
      },
      {
        heading: 'Понятные журналы',
        text: 'Исправлена конфигурация Log4j dev-запуска. Ошибка старой Twitch-интеграции остаётся известным ограничением.',
      },
      {
        heading: 'Сначала дизайн',
        text: 'Публичный сайт использует Next.js и NestJS. Новый визуальный вариант создаётся в отдельном Figma Design-файле по подробному ТЗ.',
      },
      {
        heading: 'Что дальше',
        text: 'Пользовательского установщика пока нет. Дату alpha ещё не объявляем.',
      },
    ],
  },
];
