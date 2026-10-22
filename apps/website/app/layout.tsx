import type { Metadata } from 'next';
import './globals.css';
import Header from '../components/header';
import { Footer } from '../components/shell';
export const metadata: Metadata = {
  title: 'Space Client — новая орбита твоей игры',
  description: 'Minecraft Java PvP-клиент. Следите за разработкой версии для Minecraft 1.8.9.',
  robots: { index: false, follow: false },
};
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="ru">
      <body>
        <a className="skip" href="#main">
          Перейти к содержимому
        </a>
        <Header />
        <main id="main">{children}</main>
        <Footer />
      </body>
    </html>
  );
}
