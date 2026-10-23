import { ArrowRight, Check, FolderOpen, Plus, Terminal } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { Input } from '../components/ui/input';
import { SpaceMark } from '../components/space-mark';
import { gb } from '../lib/format';
import { cn } from '../lib/utils';
import type { ReadyLauncherModel } from '../model/launcher-session';
export function ProfilesPage({ model }: { model: ReadyLauncherModel }) {
  const { snapshot, profile, draft, busy, dirty, java, session } = model;
  return (
    <div className="profiles-grid">
      <Card className="profile-list">
        <CardHeader>
          <div className="section-heading">
            <CardTitle>
              <h2>Твои профили</h2>
            </CardTitle>
            <Badge variant="secondary">{snapshot.profiles.length} / 10</Badge>
          </div>
          <CardDescription>Свои настройки для каждой игры.</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="profile-options">
            {snapshot.profiles.map((item) => (
              <Button
                variant="ghost"
                className="profile-option"
                key={item.id}
                disabled={busy || (dirty && item.id !== profile.id)}
                aria-pressed={item.id === profile.id}
                onClick={() => void session.selectProfile(item.id)}
              >
                <span className="profile-symbol">
                  <SpaceMark />
                </span>
                <span className="profile-option-copy">
                  <strong>{item.name}</strong>
                  <small>{gb(item.memoryMb)} · 1.8.9</small>
                </span>
                {item.id === profile.id && <Check className="selected-mark" aria-hidden="true" />}
              </Button>
            ))}
          </div>
          <Button
            variant="outline"
            className="w-full create-profile"
            disabled={busy || dirty || snapshot.profiles.length >= 10}
            onClick={session.createProfile}
          >
            <Plus aria-hidden="true" />
            Создать профиль
          </Button>
          {dirty && (
            <p className="inline-hint">Сохраните или отмените изменения перед сменой профиля.</p>
          )}
        </CardContent>
      </Card>
      <Card className="profile-form">
        <CardHeader>
          <div className="section-heading">
            <CardTitle>
              <h2>Настройки профиля</h2>
            </CardTitle>
            <Badge variant="outline">Локально</Badge>
          </div>
          <CardDescription>Изменения применяются после сохранения.</CardDescription>
        </CardHeader>
        <CardContent>
          <form
            onSubmit={(event) => {
              event.preventDefault();
              void session.saveProfile();
            }}
          >
            <div className="form-field">
              <label htmlFor="profile-name">Название профиля</label>
              <Input
                id="profile-name"
                maxLength={40}
                required
                disabled={busy}
                value={draft.name}
                aria-describedby="name-hint"
                onChange={(event) => session.editDraft({ name: event.target.value })}
              />
              <p id="name-hint" className="inline-hint">
                До 40 символов. Например, «Основной» или «PvP».
              </p>
            </div>
            <div className="form-field">
              <div className="range-heading">
                <label htmlFor="memory">Память для игры</label>
                <output htmlFor="memory" className="memory-value">
                  {gb(draft.memoryMb)}
                </output>
              </div>
              <input
                id="memory"
                className="memory-range"
                type="range"
                min={1024}
                max={snapshot.maxMemoryMb}
                step={256}
                disabled={busy}
                value={draft.memoryMb}
                aria-valuetext={gb(draft.memoryMb)}
                aria-describedby="memory-hint"
                onChange={(event) => session.editDraft({ memoryMb: Number(event.target.value) })}
              />
              <div className="range-limits">
                <span>1 ГБ</span>
                <span>{gb(snapshot.maxMemoryMb)}</span>
              </div>
              <p id="memory-hint" className="inline-hint">
                Всего {gb(snapshot.totalMemoryMb)}. Часть памяти оставляем системе.
              </p>
            </div>
            <div className="form-actions">
              <Button type="submit" disabled={busy || !dirty || !draft.name.trim()}>
                Сохранить изменения
              </Button>
              <Button
                type="button"
                variant="outline"
                disabled={busy || !dirty}
                onClick={session.cancelEdit}
              >
                Отменить
              </Button>
            </div>
          </form>
          <section className="java-section" aria-labelledby="java-heading">
            <div className="section-heading">
              <h3 id="java-heading">
                <Terminal aria-hidden="true" />
                Java для этого профиля
              </h3>
              <Badge variant="secondary">21 · x64</Badge>
            </div>
            <p className="file-path">{profile.javaPath || 'Файл java.exe ещё не выбран'}</p>
            <div className="form-actions">
              <Button variant="outline" disabled={busy || dirty} onClick={session.chooseJava}>
                <FolderOpen aria-hidden="true" />
                Выбрать Java
              </Button>
              <Button
                variant="secondary"
                disabled={busy || dirty || !profile.javaPath}
                onClick={session.checkJava}
              >
                Проверить Java
              </Button>
            </div>
            {java[profile.id] && (
              <p className={cn('java-result', java[profile.id].usable && 'success')} role="status">
                {java[profile.id].message}
              </p>
            )}
          </section>
          <Button
            variant="ghost"
            className="folder-action"
            disabled={busy}
            onClick={session.openFolder}
          >
            <FolderOpen aria-hidden="true" />
            Открыть папку игры
            <ArrowRight aria-hidden="true" />
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
