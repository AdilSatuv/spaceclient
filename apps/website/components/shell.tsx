import Link from 'next/link';
import { Arrow, OrbitMark } from './icons';
export function PageIntro({
  label,
  title,
  children,
}: {
  label: string;
  title: string;
  children: React.ReactNode;
}) {
  return (
    <div className="page-intro">
      <Link className="back-link" href="/">
        Главная /
      </Link>
      <p className="eyebrow">{label}</p>
      <h1>{title}</h1>
      <div className="lead">{children}</div>
      <OrbitMark className="page-orbit" />
    </div>
  );
}
export function Footer() {
  return (
    <footer className="site-footer">
      <div className="container">
        <div className="footer">
          <div>
            <Link className="brand" href="/" aria-label="Space Client — главная">
              <OrbitMark />
              <span>SPACE CLIENT</span>
            </Link>
            <p>Minecraft на своей орбите.</p>
            <small>
              Самостоятельный проект.
              <br />
              Не связан с Mojang или Microsoft.
            </small>
          </div>
          <nav aria-label="Навигация в подвале">
            <Link href="/features">Возможности</Link>
            <Link href="/download">Загрузки</Link>
            <Link href="/updates">Обновления</Link>
            <Link href="/help">Помощь</Link>
          </nav>
          <div>
            <a href="https://github.com/AdilSatuv/spaceclient">
              Исходный код <Arrow />
            </a>
            <a href="https://github.com/AdilSatuv/spaceclient/issues">
              Сообщить о проблеме <Arrow />
            </a>
            <small>AdilSatuv · 2026</small>
          </div>
        </div>
        <div className="footer-wordmark" aria-hidden="true">
          SPACE CLIENT
        </div>
        <div className="footer-bottom">
          <span>Сделано для твоего Minecraft.</span>
          <span>Пользовательская сборка ещё не выпущена.</span>
        </div>
      </div>
    </footer>
  );
}
