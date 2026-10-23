import { cn } from '../lib/utils';
export function SpaceMark({ className }: { className?: string }) {
  return (
    <svg className={cn('space-mark', className)} viewBox="0 0 40 40" fill="none" aria-hidden="true">
      <path d="m20 6 4 10 10 4-10 4-4 10-4-10-10-4 10-4Z" fill="currentColor" />
      <ellipse
        cx="20"
        cy="20"
        rx="20"
        ry="7"
        transform="rotate(-38 20 20)"
        stroke="currentColor"
        strokeWidth="1.4"
      />
    </svg>
  );
}
