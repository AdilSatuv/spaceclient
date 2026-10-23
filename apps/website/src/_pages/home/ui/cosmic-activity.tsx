'use client';

import { useEffect, useRef, type ReactNode } from 'react';

export function CosmicActivity({ children }: { children: ReactNode }) {
  const scene = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const element = scene.current;
    if (!element || !('IntersectionObserver' in window)) return;
    let inView = false;
    const update = () => {
      element.dataset.active = String(inView && !document.hidden);
    };
    const observer = new IntersectionObserver(([entry]) => {
      inView = entry.isIntersecting;
      update();
    });
    observer.observe(element);
    document.addEventListener('visibilitychange', update);
    return () => {
      observer.disconnect();
      document.removeEventListener('visibilitychange', update);
    };
  }, []);
  return (
    <div ref={scene} className="cosmic-scene" data-cosmos data-active="false" aria-hidden="true">
      {children}
    </div>
  );
}
