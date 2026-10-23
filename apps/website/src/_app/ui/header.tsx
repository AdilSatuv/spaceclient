'use client';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useEffect, useRef, useState } from 'react';
import { Arrow, OrbitMark } from '@/shared/ui/icons';
import { SpaceMotion } from '@/features/motion-preference';
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
  const brand = useRef<HTMLAnchorElement>(null);
  const desktopNav = useRef<HTMLElement>(null);
  const [menuOpen, setMenuOpen] = useState(false);
  const close = () => dialog.current?.close();

  useEffect(() => {
    if (!menuOpen) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [menuOpen]);

  useEffect(() => {
    const desktop = window.matchMedia('(min-width: 761px)');
    const closeOnDesktop = () => {
      if (desktop.matches) dialog.current?.close();
    };
    desktop.addEventListener('change', closeOnDesktop);
    return () => desktop.removeEventListener('change', closeOnDesktop);
  }, []);

  useEffect(() => {
    dialog.current?.close();
  }, [path]);
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
        <Link className="brand" href="/" aria-label="Space Client — главная" ref={brand}>
          <OrbitMark />
          <span>SPACE CLIENT</span>
        </Link>
        <nav className="desktop-nav" aria-label="Главная навигация" ref={desktopNav}>
          {nav}
        </nav>
        <SpaceMotion />
        <a className="github-link" href="https://github.com/AdilSatuv/spaceclient">
          GitHub <Arrow />
        </a>
        <button
          className="menu-trigger secondary"
          ref={trigger}
          aria-haspopup="dialog"
          aria-expanded={menuOpen}
          aria-controls="mobile-navigation"
          onClick={() => {
            dialog.current?.showModal();
            setMenuOpen(true);
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
          id="mobile-navigation"
          ref={dialog}
          aria-labelledby="menu-title"
          onKeyDown={(event) => {
            if (event.key !== 'Tab') return;
            const controls = Array.from(
              event.currentTarget.querySelectorAll<HTMLElement>('button, a[href]'),
            ).filter((control) => control.getClientRects().length > 0);
            const first = controls[0];
            const last = controls.at(-1);
            if (event.shiftKey && document.activeElement === first) {
              event.preventDefault();
              last?.focus();
            } else if (!event.shiftKey && document.activeElement === last) {
              event.preventDefault();
              first?.focus();
            }
          }}
          onClose={() => {
            setMenuOpen(false);
            const focusTarget = trigger.current?.getClientRects().length
              ? trigger.current
              : (desktopNav.current?.querySelector<HTMLAnchorElement>('[aria-current="page"]') ??
                brand.current);
            focusTarget?.focus();
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
