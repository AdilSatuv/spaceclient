import type { Metadata } from 'next';
import Header from './header';
import { Footer } from './footer';
import { pageMetadata } from '@/shared/lib/metadata';
export const metadata: Metadata = pageMetadata(
  '/',
  'Space Client — новая орбита твоей игры',
  'Minecraft Java PvP-клиент. Следите за разработкой версии для Minecraft 1.8.9.',
);
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="ru" data-motion="paused">
      <body>
        <a className="skip" href="#main">
          Перейти к содержимому
        </a>
        <Header />
        <main id="main" tabIndex={-1}>
          {children}
        </main>
        <Footer />
      </body>
    </html>
  );
}
