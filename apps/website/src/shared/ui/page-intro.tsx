import Link from 'next/link';
import { OrbitMark } from './icons';
export function PageIntro({
  label,
  title,
  children,
}: {
  label: string;
  title: string;
  children: React.ReactNode;
}) {
  return (
    <div className="page-intro">
      <Link className="back-link" href="/">
        Главная /
      </Link>
      <p className="eyebrow">{label}</p>
      <h1>{title}</h1>
      <div className="lead">{children}</div>
      <OrbitMark className="page-orbit" />
    </div>
  );
}
