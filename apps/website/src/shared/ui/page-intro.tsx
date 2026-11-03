import Link from './site-link';
import { OrbitMark } from './icons';
export function PageIntro({
  section,
  label,
  title,
  children,
}: {
  section: string;
  label: string;
  title: string;
  children: React.ReactNode;
}) {
  return (
    <div className="page-intro">
      <nav className="page-location" aria-label="Путь к странице">
        <Link className="back-link" href="/">
          Главная
        </Link>
        <span aria-hidden="true">/</span>
        <span aria-current="page">{section}</span>
      </nav>
      <p className="eyebrow">{label}</p>
      <h1>{title}</h1>
      <div className="lead">{children}</div>
      <OrbitMark className="page-orbit" />
    </div>
  );
}
