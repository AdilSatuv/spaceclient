'use client';

import Link, { useLinkStatus } from 'next/link';
import type { ComponentProps } from 'react';

function NavigationFeedback() {
  const { pending } = useLinkStatus();
  return (
    <span className="navigation-feedback" data-pending={pending} role="status" aria-live="polite">
      <span className="visually-hidden">{pending ? 'Открываем страницу…' : ''}</span>
    </span>
  );
}

export default function SiteLink({
  children,
  className = '',
  ...props
}: ComponentProps<typeof Link>) {
  return (
    <Link {...props} className={'route-link ' + className}>
      {children}
      <NavigationFeedback />
    </Link>
  );
}
