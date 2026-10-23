import Image from 'next/image';
import { OrbitMark } from './icons';
export default function HudPreview() {
  return (
    <figure className="hud-preview">
      <div className="preview-chrome">
        <span>
          <OrbitMark /> SPACE / HUD
        </span>
        <span className="concept-label">Концепт интерфейса</span>
      </div>
      <div className="hud-scene">
        <Image
          src="/images/space-hero-v2.png"
          alt=""
          fill
          sizes="(max-width: 760px) 100vw, 1200px"
          quality={75}
        />
        <div className="hud-scene-shade" />
        <div className="hud-widgets">
          <div className="hud-widget">
            <small>PERFORMANCE</small>
            <strong>
              FPS <span>—</span>
            </strong>
            <span className="widget-note">значение из игры</span>
          </div>
          <div className="hud-widget">
            <small>COORDINATES</small>
            <strong>
              X <span>—</span> Y <span>—</span> Z <span>—</span>
            </strong>
          </div>
        </div>
        <span className="crosshair" aria-hidden="true">
          +
        </span>
        <div className="hud-keys" aria-label="Иллюстрация виджета клавиш">
          <kbd>W</kbd>
          <div>
            <kbd>A</kbd>
            <kbd className="lit">S</kbd>
            <kbd>D</kbd>
          </div>
          <kbd className="space-key">SPACE</kbd>
        </div>
        <div className="hud-hotbar" aria-hidden="true">
          {Array.from({ length: 9 }, (_, i) => (
            <span key={i} />
          ))}
        </div>
        <span className="scene-corner">SPACE CLIENT / HUD CONCEPT</span>
      </div>
      <figcaption>
        Иллюстрация будущего интерфейса. Не скриншот из игры; показатели производительности не
        измерены.
      </figcaption>
    </figure>
  );
}
