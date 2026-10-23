import type { NextConfig } from 'next';
const config: NextConfig = {
  poweredByHeader: false,
  images: { qualities: [75, 80, 85] },
  // Small read-only pages share their bounded API request with metadata. Emit
  // metadata in the head for every visitor; streaming it caused late shell swaps.
  htmlLimitedBots: /.*/,
};
export default config;
