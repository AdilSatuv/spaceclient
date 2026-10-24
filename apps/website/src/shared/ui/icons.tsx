import { useId } from 'react';

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
  const id = useId();
  return (
    <svg
      className={'ringworld-mark ' + className}
      viewBox="0 0 48 48"
      fill="none"
      aria-hidden="true"
    >
      <defs>
        <radialGradient id={id + '-sphere'} cx="29%" cy="22%" r="78%">
          <stop stopColor="#fff2cf" />
          <stop offset=".28" stopColor="#f1c18a" />
          <stop offset=".64" stopColor="#b87446" />
          <stop offset=".87" stopColor="#613526" />
          <stop offset="1" stopColor="#29191d" />
        </radialGradient>
        <linearGradient
          id={id + '-ring'}
          x1="1"
          y1="27"
          x2="44"
          y2="20"
          gradientUnits="userSpaceOnUse"
        >
          <stop stopColor="#6a3b29" />
          <stop offset=".22" stopColor="#c88b55" />
          <stop offset=".5" stopColor="#ffe5b4" />
          <stop offset=".74" stopColor="#e3ac72" />
          <stop offset="1" stopColor="#8b5231" />
        </linearGradient>
      </defs>
      <g className="ringworld-rich">
        <ellipse
          cx="24"
          cy="24"
          rx="23"
          ry="6"
          transform="rotate(-28 24 24)"
          stroke={'url(#' + id + '-ring)'}
          strokeWidth="2.5"
          opacity=".72"
        />
        <circle
          cx="24"
          cy="24"
          r="12.5"
          fill={'url(#' + id + '-sphere)'}
          stroke="#d39a63"
          strokeWidth=".45"
        />
        <path
          d="M13.5 19A12 12 0 0 1 28 12.8"
          stroke="#fff2cc"
          strokeWidth=".7"
          strokeLinecap="round"
          opacity=".7"
        />
        <g transform="rotate(-28 24 24)">
          <path d="M1 25.2a23 6 0 0 0 46 0" stroke="#48291f" strokeWidth="3.4" />
          <path d="M1 24a23 6 0 0 0 46 0" stroke={'url(#' + id + '-ring)'} strokeWidth="3.2" />
          <path d="M2 23.5a22 5.6 0 0 0 44 0" stroke="#ffebc5" strokeWidth=".65" opacity=".8" />
        </g>
      </g>
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
