import Retry from './retry';
export default function ContentError({
  title = 'Не удалось загрузить данные.',
}: {
  title?: string;
}) {
  return (
    <section className="empty-state" aria-labelledby="content-error-title">
      <p className="eyebrow">СВЯЗЬ ПРЕРВАЛАСЬ</p>
      <h2 id="content-error-title">{title}</h2>
      <p>
        Сервис сейчас недоступен или вернул неполные данные. Это не означает, что каталог пуст.
        Попробуйте ещё раз позже.
      </p>
      <Retry />
    </section>
  );
}
