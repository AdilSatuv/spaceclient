'use client';

import { useEffect, useRef, useState } from 'react';
import type { ReactNode } from 'react';

export function FilterForm({ action, children }: { action: string; children: ReactNode }) {
  const form = useRef<HTMLFormElement>(null);
  const [submitted, setSubmitted] = useState(false);
  useEffect(() => {
    let timer: ReturnType<typeof setTimeout>;
    const restore = () => {
      clearTimeout(timer);
      setSubmitted(false);
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
    <form
      ref={form}
      className="filter-form"
      action={action}
      method="get"
      autoComplete="off"
      onSubmit={() => setSubmitted(true)}
    >
      {children}
      <p className="filter-status" role="status">
        {submitted
          ? 'Запрос отправлен. Пока показано прежнее состояние. Если ответ не появился, попробуйте снова.'
          : ''}
      </p>
      <noscript>
        <p className="filter-status">
          Показанное состояние соответствует адресу страницы. При возврате браузер может
          восстановить ввод; чтобы применить значения полей, нажмите кнопку.
        </p>
      </noscript>
    </form>
  );
}
