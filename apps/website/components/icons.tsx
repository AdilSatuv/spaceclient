export function Arrow() {
  return (
    <svg
      className="icon"
      width="18"
      height="18"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      aria-hidden="true"
    >
      <path d="M6 18 18 6M6 6h12v12" />
    </svg>
  );
}
export function OrbitMark({ className = '' }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 48 48" fill="none" aria-hidden="true">
      <path d="m24 7 4.5 12.5L41 24l-12.5 4.5L24 41l-4.5-12.5L7 24l12.5-4.5Z" fill="currentColor" />
      <ellipse
        cx="24"
        cy="24"
        rx="24"
        ry="8"
        transform="rotate(-42 24 24)"
        stroke="currentColor"
        strokeWidth="1.4"
      />
    </svg>
  );
}
