'use client';

import { useEffect, useRef, useState } from 'react';
import { usePathname } from 'next/navigation';

const preferenceKey = 'space.motion';

export function SpaceMotion() {
  const path = usePathname();
  const [paused, setPaused] = useState(false);
  const [reduced, setReduced] = useState(false);
  const [ready, setReady] = useState(false);
  const seen = useRef(new WeakSet<Element>());

  useEffect(() => {
    const preference = window.matchMedia('(prefers-reduced-motion: reduce)');
    const update = () => setReduced(preference.matches);
    update();
    try {
      setPaused(window.localStorage.getItem(preferenceKey) === 'paused');
    } catch {
      // The control still works for this visit when browser storage is unavailable.
    }
    setReady(true);
    preference.addEventListener('change', update);
    return () => preference.removeEventListener('change', update);
  }, []);

  useEffect(() => {
    document.documentElement.dataset.motion = !ready || paused || reduced ? 'paused' : 'active';
    if (!ready || paused || reduced || !('IntersectionObserver' in window)) return;
    const animations = new Set<Animation>();
    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (!entry.isIntersecting || seen.current.has(entry.target)) continue;
          seen.current.add(entry.target);
          observer.unobserve(entry.target);
          const animation = entry.target.animate(
            [
              { opacity: 0.3, transform: 'translateY(22px)' },
              { opacity: 1, transform: 'none' },
            ],
            { duration: 650, easing: 'cubic-bezier(.2,.7,.2,1)' },
          );
          animations.add(animation);
          animation.onfinish = () => animations.delete(animation);
        }
      },
      { threshold: 0.1 },
    );
    document
      .querySelectorAll('[data-reveal], .page-intro, .catalog > *, .article-links > *')
      .forEach((element) => observer.observe(element));
    return () => {
      observer.disconnect();
      animations.forEach((animation) => animation.cancel());
    };
  }, [path, paused, reduced, ready]);

  return (
    <button
      className="motion-toggle"
      aria-label={
        reduced
          ? 'Движение уменьшено настройкой системы'
          : paused
            ? 'Включить анимации'
            : 'Приостановить анимации'
      }
      aria-pressed={paused || reduced}
      disabled={!ready || reduced}
      onClick={() => {
        const nextPaused = !paused;
        setPaused(nextPaused);
        try {
          window.localStorage.setItem(preferenceKey, nextPaused ? 'paused' : 'active');
        } catch {
          // Storage restrictions must not prevent pausing the current page.
        }
      }}
      title={
        reduced
          ? 'Используется настройка уменьшения движения системы'
          : paused
            ? 'Включить анимации'
            : 'Приостановить анимации'
      }
    >
      <svg viewBox="0 0 20 20" width="16" height="16" aria-hidden="true">
        {paused || reduced ? (
          <path d="m6 3 10 7-10 7Z" fill="currentColor" />
        ) : (
          <path d="M5 4h3v12H5zm7 0h3v12h-3z" fill="currentColor" />
        )}
      </svg>
      <span>
        {reduced ? 'Движение уменьшено' : paused ? 'Включить анимации' : 'Пауза анимаций'}
      </span>
    </button>
  );
}
