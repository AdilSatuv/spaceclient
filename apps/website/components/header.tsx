'use client';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useRef } from 'react';
import { Arrow, OrbitMark } from './icons';
const links = [
  ['/features', 'Возможности'],
  ['/download', 'Загрузки'],
  ['/updates', 'Обновления'],
  ['/help', 'Помощь'],
];
export default function Header() {
  const path = usePathname();
  const dialog = useRef<HTMLDialogElement>(null);
  const trigger = useRef<HTMLButtonElement>(null);
  const close = () => dialog.current?.close();
  const nav = links.map(([href, label]) => (
    <Link
      key={href}
      href={href}
      aria-current={path.startsWith(href) ? 'page' : undefined}
      onClick={close}
    >
      {label}
    </Link>
  ));
  return (
    <header className="site-header">
      <div className="container header-inner">
        <Link className="brand" href="/" aria-label="Space Client — главная">
          <OrbitMark />
          <span>SPACE CLIENT</span>
        </Link>
        <nav className="desktop-nav" aria-label="Главная навигация">
          {nav}
        </nav>
        <a className="github-link" href="https://github.com/AdilSatuv/spaceclient">
          GitHub <Arrow />
        </a>
        <button
          className="menu-trigger secondary"
          ref={trigger}
          onClick={() => {
            dialog.current?.showModal();
            document.body.style.overflow = 'hidden';
          }}
        >
          Меню{' '}
          <svg
            className="icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            aria-hidden="true"
          >
            <path d="M4 8h16M4 16h16" />
          </svg>
        </button>
        <dialog
          ref={dialog}
          aria-labelledby="menu-title"
          onClose={() => {
            document.body.style.overflow = '';
            trigger.current?.focus();
          }}
        >
          <div className="menu-top">
            <strong id="menu-title">Навигация</strong>
            <button className="secondary" onClick={close} autoFocus>
              Закрыть ×
            </button>
          </div>
          <nav aria-label="Мобильная навигация">
            {nav}
            <a href="https://github.com/AdilSatuv/spaceclient">
              GitHub <Arrow />
            </a>
          </nav>
          <p className="caption">Space Client · В разработке</p>
        </dialog>
      </div>
    </header>
  );
}
