import type { Metadata } from 'next';
import './globals.css';
export const metadata: Metadata = {
  title: 'Space Client — новая орбита твоей игры',
  description: 'Minecraft Java PvP-клиент. Следите за разработкой версии для Minecraft 1.8.9.'
};
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="ru"><body>{children}</body></html>;
}
