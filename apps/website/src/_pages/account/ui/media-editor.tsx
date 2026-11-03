'use client';
import { useEffect, useState } from 'react';
import type { SpaceAccountProfile, SkinModel } from '@space/account-contracts';
import { MAX_IMAGE_BYTES } from '@space/account-contracts';
import { accountAssetUrl } from '@/shared/api/index.client';
import SkinPreview from './skin-preview';
type Props = {
  kind: 'skin' | 'cape';
  profile: SpaceAccountProfile;
  disabled: boolean;
  save: (action: 'skin' | 'cape' | 'deleteSkin' | 'deleteCape', body: unknown) => Promise<boolean>;
  fail: (message: string) => void;
};
export default function MediaEditor({ kind, profile, disabled, save, fail }: Props) {
  const [file, setFile] = useState<File | null>(null),
    [preview, setPreview] = useState(''),
    [model, setModel] = useState<SkinModel>(profile.skin?.model ?? 'classic');
  useEffect(() => {
    if (!file) setModel(profile.skin?.model ?? 'classic');
  }, [profile.skin?.model, file]);
  useEffect(() => {
    if (!file) {
      setPreview('');
      return;
    }
    const url = URL.createObjectURL(new Blob([file], { type: 'image/png' }));
    setPreview(url);
    return () => URL.revokeObjectURL(url);
  }, [file]);
  const asset = profile[kind],
    source = preview || (asset ? accountAssetUrl(asset.sha256) : ''),
    title = kind === 'skin' ? 'Скин' : 'Плащ';
  return (
    <section className="page-panel account-media" aria-labelledby={`${kind}-heading`}>
      <h2 id={`${kind}-heading`}>{title}</h2>
      <div className="appearance-preview">
        {source ? (
          kind === 'skin' ? (
            <SkinPreview src={source} slim={model === 'slim'} />
          ) : (
            <img src={source} width={256} height={128} alt="Предпросмотр текстуры плаща" />
          )
        ) : (
          <p className="muted">{title} пока не выбран</p>
        )}
      </div>
      <p className="caption">
        {preview
          ? 'Предпросмотр выбранного файла. Ещё не сохранено.'
          : asset
            ? 'Сохранено в аккаунте Space.'
            : 'Можно загрузить собственную PNG-текстуру.'}
      </p>
      <form
        onSubmit={async (event) => {
          event.preventDefault();
          if (!file) {
            fail('Сначала выберите PNG-файл.');
            return;
          }
          if (file.size > MAX_IMAGE_BYTES) {
            fail('PNG должен быть не больше 256 КиБ.');
            return;
          }
          try {
            const bytes = new Uint8Array(await file.arrayBuffer());
            let binary = '';
            for (const byte of bytes) binary += String.fromCharCode(byte);
            if (
              await save(kind, {
                pngBase64: btoa(binary),
                expectedRevision: profile.revision,
                ...(kind === 'skin' ? { model } : {}),
              })
            )
              setFile(null);
          } catch {
            fail('Не удалось прочитать выбранный файл.');
          }
        }}
      >
        <fieldset disabled={disabled}>
          <legend className="visually-hidden">Изменить {title.toLowerCase()}</legend>
          <label htmlFor={`${kind}-file`}>
            PNG-файл
            <input
              id={`${kind}-file`}
              name={kind}
              type="file"
              accept="image/png,.png"
              onChange={(event) => {
                const selected = event.target.files?.[0] ?? null;
                if (selected && selected.size > MAX_IMAGE_BYTES) {
                  fail('PNG должен быть не больше 256 КиБ.');
                  event.target.value = '';
                  setFile(null);
                  return;
                }
                setFile(selected);
              }}
              aria-describedby={`${kind}-help`}
            />
          </label>
          <p id={`${kind}-help`} className="caption">
            {kind === 'skin'
              ? '64×64 classic/slim или 64×32 classic.'
              : '64×32, 128×64 или 256×128.'}{' '}
            До 256 КиБ. Сервер проверит весь PNG.
          </p>
          {kind === 'skin' && (
            <label htmlFor="skin-model">
              Модель рук
              <select
                id="skin-model"
                value={model}
                onChange={(event) => setModel(event.target.value as SkinModel)}
              >
                <option value="classic">Classic — обычные</option>
                <option value="slim">Slim — узкие</option>
              </select>
            </label>
          )}
          <div className="account-actions">
            <button className="button" type="submit" disabled={!file}>
              Сохранить {title.toLowerCase()}
            </button>
            {asset && (
              <button
                className="button secondary"
                type="button"
                onClick={() =>
                  void save(kind === 'skin' ? 'deleteSkin' : 'deleteCape', {
                    expectedRevision: profile.revision,
                  })
                }
              >
                Удалить {title.toLowerCase()}
              </button>
            )}
          </div>
        </fieldset>
      </form>
    </section>
  );
}
