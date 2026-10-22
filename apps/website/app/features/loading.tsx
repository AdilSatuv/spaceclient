export default function Loading() {
  return (
    <div className="container page-content loading-content" role="status">
      <p className="eyebrow">Загружаем страницу…</p>
      <div className="skeleton" />
    </div>
  );
}
