import type { NextConfig } from 'next';
const config: NextConfig = {
  poweredByHeader: false,
  images: { qualities: [75, 80, 85] },
  async headers() {
    return [
      {
        source: '/:path*',
        headers: [
          { key: 'X-Content-Type-Options', value: 'nosniff' },
          { key: 'X-Frame-Options', value: 'DENY' },
          { key: 'Referrer-Policy', value: 'strict-origin-when-cross-origin' },
          {
            key: 'Permissions-Policy',
            value: 'camera=(), microphone=(), geolocation=(), payment=()',
          },
          // Narrow policy compatible with Next's inline hydration scripts. TLS/HSTS belongs to the proxy.
          {
            key: 'Content-Security-Policy',
            value: "object-src 'none'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'",
          },
        ],
      },
    ];
  },
  // Small read-only pages share their bounded API request with metadata. Emit
  // metadata in the head for every visitor; streaming it caused late shell swaps.
  htmlLimitedBots: /.*/,
};
export default config;
