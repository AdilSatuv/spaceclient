'use client';
import { useState } from 'react';
export default function CopyHash({ hash }: { hash: string }) {
  const [message, setMessage] = useState('');
  const [pending, setPending] = useState(false);
  async function copy() {
    setPending(true);
    try {
      await navigator.clipboard.writeText(hash);
      setMessage('Скопировано');
    } catch {
      setMessage('Не удалось скопировать. Выделите хеш и скопируйте вручную.');
    } finally {
      setPending(false);
    }
  }
  return (
    <div className="hash-block">
      <label>
        SHA-256
        <input aria-label="SHA-256 артефакта" readOnly value={hash} spellCheck={false} />
      </label>
      <button className="button secondary" disabled={pending} onClick={copy}>
        {pending ? 'Копируем…' : 'Копировать хеш'}
      </button>
      <span className="caption" role="status">
        {message}
      </span>
    </div>
  );
}
