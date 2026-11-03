import { PageIntro } from '@/shared/ui/page-intro';
import AccountPanel from './account-panel';
import { pageMetadata } from '@/shared/lib/metadata';
export function generateMetadata() {
  return pageMetadata(
    '/account',
    'Аккаунт Space',
    'Вход, регистрация и профиль Space: ник, скин и плащ для сайта и лаунчера.',
    true,
  );
}
export default function AccountPage() {
  return (
    <div className="container page-content account-page">
      <PageIntro section="Аккаунт" label="ТВОЁ МЕСТО В SPACE" title="Один профиль. Твой стиль.">
        Аккаунт Space объединяет ник, скин и плащ на сайте и в лаунчере. Вход Microsoft и публичные
        серверы — отдельная история.
      </PageIntro>
      <AccountPanel />
      <noscript>
        <p className="empty-state">
          Для входа и изменения профиля включите JavaScript. Пароль не отправляется без вашего
          действия.
        </p>
      </noscript>
    </div>
  );
}
