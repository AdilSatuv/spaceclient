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
    <svg
      className={'ringworld-mark ' + className}
      viewBox="0 0 48 48"
      fill="none"
      aria-hidden="true"
    >
      <image
        className="ringworld-rich"
        href="/images/space-logo-warm-web.webp"
        width="48"
        height="48"
      />
      <g className="ringworld-flat">
        <circle cx="24" cy="24" r="12" fill="currentColor" />
        <ellipse
          cx="24"
          cy="24"
          rx="23"
          ry="6"
          transform="rotate(-28 24 24)"
          stroke="Canvas"
          strokeWidth="5"
        />
        <ellipse
          cx="24"
          cy="24"
          rx="23"
          ry="6"
          transform="rotate(-28 24 24)"
          stroke="currentColor"
          strokeWidth="2"
        />
      </g>
    </svg>
  );
}
