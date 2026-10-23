import Link from 'next/link';
import { Arrow } from '@/shared/ui/icons';
import HudPreview from './hud-preview';
import { CosmicScene } from './cosmic-scene';
const steps = [
  {
    n: '01',
    title: 'Посмотри, что внутри.',
    text: 'Познакомься с модулями и их статусами. Начинаем с небольших деталей, которые помогают в игре.',
    href: '/features',
    link: 'Возможности',
  },
  {
    n: '02',
    title: 'Проверь совместимость.',
    text: 'Первая цель — Minecraft Java 1.8.9 и Windows x64. Требования уточним перед первым релизом.',
    href: '/download',
    link: 'К загрузкам',
  },
  {
    n: '03',
    title: 'Оставайся на орбите.',
    text: 'Следи за реальными изменениями. О готовности первой сборки расскажем в обновлениях.',
    href: '/updates',
    link: 'Ход разработки',
  },
];
export default function Home() {
  return (
    <>
      <section className="hero">
        <div className="hero-shade" />
        <CosmicScene />
        <div className="container hero-content">
          <p className="eyebrow rule-label">SPACE CLIENT / MINECRAFT НА СВОЕЙ ОРБИТЕ</p>
          <h1>
            ТВОЙ МИР.
            <br />
            <span className="orbit-title">НОВАЯ ОРБИТА.</span>
          </h1>
          <p className="lead">
            Знакомый блочный мир. Свой взгляд на игру.
            <br />
            Space Client — Minecraft-клиент с HUD и модулями,
            <br className="desktop-break" /> которые ты настроишь под себя.
          </p>
          <div className="actions">
            <Link className="button" href="/download">
              К загрузкам <Arrow />
            </Link>
            <Link className="button secondary" href="/features">
              Возможности <Arrow />
            </Link>
          </div>
          <p className="hero-status">
            <i /> В разработке <span>·</span> Minecraft 1.8.9 <span>·</span> Windows x64
          </p>
        </div>
        <div className="container hero-bottom">
          <span>01 / ИССЛЕДУЙ НОВУЮ ОРБИТУ</span>
          <span className="concept-label">ECLIPSE / 01</span>
        </div>
      </section>
      <div className="capability-strip" data-reveal>
        <div className="container capability-grid">
          <div>
            <span>01</span>
            <p>
              Информация в игре<small>FPS и координаты · Прототипы</small>
            </p>
          </div>
          <div>
            <span>02</span>
            <p>
              Движение на виду<small>Keystrokes · Прототип</small>
            </p>
          </div>
          <div>
            <span>03</span>
            <p>
              Твоя композиция<small>Редактор HUD · В планах</small>
            </p>
          </div>
        </div>
      </div>
      <section className="container section" data-reveal>
        <div className="section-heading">
          <div>
            <p className="eyebrow">02 / В ЦЕНТРЕ — ТВОЙ МИР</p>
            <h2>
              Меньше лишнего.
              <br className="mobile-break" /> Больше игры.
            </h2>
          </div>
          <p>
            Важные данные перед глазами.
            <br />
            Остальное пространство — для Minecraft.
          </p>
        </div>
        <HudPreview />
        <div className="section-bottom">
          <p>
            FPS, координаты и клавиши уже есть в dev-прототипе.
            <br />
            Редактор расположения и стиля — следующий этап.
          </p>
          <Link className="text-link" href="/features">
            Исследовать возможности <Arrow />
          </Link>
        </div>
      </section>
      <section className="steps-section" data-reveal>
        <div className="container section">
          <p className="eyebrow">03 / ПЕРВОЕ ЗНАКОМСТВО</p>
          <h2>
            С чего начинается
            <br />
            твоя орбита.
          </h2>
          <div className="steps">
            {steps.map((item) => (
              <article key={item.n}>
                <span className="step-number">{item.n}</span>
                <h3>{item.title}</h3>
                <p>{item.text}</p>
                <Link className="text-link" href={item.href}>
                  {item.link} <Arrow />
                </Link>
              </article>
            ))}
          </div>
        </div>
      </section>
      <section className="container section" data-reveal>
        <Link className="update-banner" href="/updates/launcher-and-website">
          <div>
            <p className="eyebrow">БОРТОВОЙ ЖУРНАЛ</p>
            <time dateTime="2026-10-24">24.10.2026</time>
            <span className="status">Разработка</span>
          </div>
          <div>
            <h2>
              Лаунчер обретает форму.
              <br />
              Дальше — первый запуск.
            </h2>
            <p>
              Профили, Java и подготовка файлов уже проверены. Рассказываем о прогрессе и о том, что
              нужно до первой пользовательской сборки.
            </p>
            <span className="text-link">
              Читать заметку <Arrow />
            </span>
          </div>
        </Link>
      </section>
      <section className="container section faq-layout" data-reveal>
        <div>
          <p className="eyebrow">04 / ЧАСТЫЕ ВОПРОСЫ</p>
          <h2>
            На одной <br />
            орбите.
          </h2>
          <p className="muted">Всё, что стоит знать перед стартом.</p>
          <Link className="text-link" href="/help">
            Центр помощи <Arrow />
          </Link>
        </div>
        <div>
          <details>
            <summary>Можно уже скачать Space Client?</summary>
            <p>
              Пользовательский установщик ещё не выпущен. Проверенные сборки появятся на странице
              загрузок после испытаний.
            </p>
          </details>
          <details>
            <summary>Какие версии поддерживаются?</summary>
            <p>
              Начинаем с Minecraft Java 1.8.9 и Windows x64. Это цели первой версии; готовых сборок
              для других ОС пока нет.
            </p>
          </details>
          <details>
            <summary>Будет ли больше FPS?</summary>
            <p>
              Сравнительных измерений пока нет. Сначала проверим стабильность, затем измерим FPS и
              время кадра в одинаковых условиях.
            </p>
          </details>
          <details>
            <summary>Что уже есть в клиенте?</summary>
            <p>
              В dev-моде есть FPS, CPS, координаты и состояния клавиш; часть игровых проверок ещё
              впереди. В прототипе лаунчера проверены профили, Java и подготовка файлов. Вход,
              запуск игры и редактор HUD ещё разрабатываются.
            </p>
          </details>
        </div>
      </section>
    </>
  );
}
