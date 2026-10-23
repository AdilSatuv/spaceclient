'use client';

import { useEffect, useRef } from 'react';
import type { ReactNode } from 'react';

export function FilterForm({ action, children }: { action: string; children: ReactNode }) {
  const form = useRef<HTMLFormElement>(null);
  useEffect(() => {
    let timer: ReturnType<typeof setTimeout>;
    const restore = () => {
      clearTimeout(timer);
      // History can restore an unsubmitted draft after pageshow/popstate.
      // Wait until that finishes, then use the server-rendered query defaults.
      timer = setTimeout(() => form.current?.reset(), 0);
    };
    restore();
    window.addEventListener('pageshow', restore);
    window.addEventListener('popstate', restore);
    return () => {
      clearTimeout(timer);
      window.removeEventListener('pageshow', restore);
      window.removeEventListener('popstate', restore);
    };
  }, []);
  return (
    <form ref={form} className="filter-form" action={action} autoComplete="off">
      {children}
    </form>
  );
}
