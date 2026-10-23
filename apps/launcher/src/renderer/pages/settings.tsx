import { FolderOpen, HardDrive, LockKeyhole, ShieldCheck, Terminal } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { gb } from '../lib/format';
import { cn } from '../lib/utils';
import type { ReadyLauncherModel } from '../model/launcher-session';
export function SettingsPage({ model }: { model: ReadyLauncherModel }) {
  const { snapshot, profile, busy, java, javaLabel, session } = model;
  return (
    <>
      <div className="settings-grid">
        <Card>
          <CardHeader>
            <span className="section-icon">
              <Terminal aria-hidden="true" />
            </span>
            <CardTitle>
              <h2>Среда для игры</h2>
            </CardTitle>
            <CardDescription>Проверка Java в активном профиле.</CardDescription>
          </CardHeader>
          <CardContent>
            <dl className="diagnostic-list">
              <div>
                <dt>Профиль</dt>
                <dd>{profile.name}</dd>
              </div>
              <div>
                <dt>Память устройства</dt>
                <dd>{gb(snapshot.totalMemoryMb)}</dd>
              </div>
              <div>
                <dt>Выделено игре</dt>
                <dd>{gb(profile.memoryMb)}</dd>
              </div>
              <div>
                <dt>Java</dt>
                <dd>{javaLabel}</dd>
              </div>
            </dl>
            <Button disabled={busy || !profile.javaPath} onClick={session.checkJava}>
              <ShieldCheck aria-hidden="true" />
              Проверить Java
            </Button>
            {!profile.javaPath && (
              <p className="inline-hint">Сначала выберите java.exe в разделе «Профили».</p>
            )}
            {java[profile.id] && (
              <p className={cn('java-result', java[profile.id].usable && 'success')} role="status">
                {java[profile.id].message}
              </p>
            )}
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <span className="section-icon">
              <HardDrive aria-hidden="true" />
            </span>
            <CardTitle>
              <h2>Хранилище</h2>
            </CardTitle>
            <CardDescription>Настройки и игровые файлы на устройстве.</CardDescription>
          </CardHeader>
          <CardContent>
            <p className="field-caption">Папка данных лаунчера</p>
            <p className="file-path storage-path">{snapshot.dataRoot}</p>
            <Button variant="outline" disabled={busy} onClick={session.openFolder}>
              <FolderOpen aria-hidden="true" />
              Папка текущего профиля
            </Button>
            <p className="inline-hint storage-hint">
              Каждый профиль получает отдельную папку. Лаунчер не использует .minecraft других
              установок.
            </p>
          </CardContent>
        </Card>
      </div>
      <Card className="account-notice">
        <CardContent>
          <span className="section-icon">
            <LockKeyhole aria-hidden="true" />
          </span>
          <div>
            <h2>Microsoft-аккаунт</h2>
            <p>Вход и проверка владения Minecraft ещё в разработке. Запуск игры пока недоступен.</p>
          </div>
          <Badge variant="outline">В разработке</Badge>
        </CardContent>
      </Card>
    </>
  );
}
