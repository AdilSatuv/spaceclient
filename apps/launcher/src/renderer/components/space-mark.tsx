// The selected Space logo is shared with the website; the vector fallback remains legible in forced colors.
export function SpaceMark({ className = '' }: { className?: string }) {
  return (
    <span className={'space-mark ringworld-mark ' + className} aria-hidden="true">
      <img className="ringworld-rich" src="space://app/logo.png" alt="" width="48" height="48" />
      <svg className="ringworld-flat" viewBox="0 0 48 48" fill="none">
        <circle cx="24" cy="24" r="19" fill="currentColor" />
        <ellipse
          cx="24"
          cy="24"
          rx="22"
          ry="7"
          transform="rotate(-24 24 24)"
          stroke="Canvas"
          strokeWidth="4"
        />
        <ellipse
          cx="24"
          cy="24"
          rx="22"
          ry="7"
          transform="rotate(-24 24 24)"
          stroke="currentColor"
          strokeWidth="2"
        />
      </svg>
    </span>
  );
}
