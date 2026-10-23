import { CosmicActivity } from './cosmic-activity';

function Fragment({ className }: { className: string }) {
  return (
    <svg className={className} viewBox="0 0 60 64" fill="none">
      <path className="fragment-top" d="m30 4 24 14-24 14L6 18Z" />
      <path className="fragment-left" d="m6 18 24 14v28L6 46Z" />
      <path className="fragment-right" d="m30 32 24-14v28L30 60Z" />
    </svg>
  );
}

export function CosmicScene() {
  return (
    <CosmicActivity>
      <div className="cosmic-horizon" />
      <div className="shooting-star" />
      <div className="orbital-system">
        <div className="planet-assembly">
          <div className="planet-aura" />
          <div className="eclipse-planet">
            <div className="planet-surface">
              <svg className="planet-terrain" viewBox="0 0 200 200" fill="none">
                <path
                  className="terrain-highland"
                  d="m10 33 28-15 18 12 22-8 13 23-9 18 16 21-22 19-8 27-25 10-19-21 7-28-16-17Z"
                />
                <path
                  className="terrain-lowland"
                  d="m117 3 16 27-9 23 25 12 5 28 30 14 20-11V0ZM105 116l26-18 23 11-3 24 25 17-8 37-25 15-35-18 6-27-16-18Z"
                />
                <path
                  className="terrain-fault"
                  d="m76 17 13 28-7 18 16 21-22 19-8 27 13 28-7 30M134 30l-10 23 25 12 5 28 30 14M104 117l27-19 23 11"
                />
                <g className="terrain-craters">
                  <ellipse cx="50" cy="63" rx="12" ry="8" />
                  <ellipse cx="126" cy="126" rx="17" ry="12" />
                  <ellipse cx="77" cy="149" rx="7" ry="5" />
                  <ellipse cx="150" cy="47" rx="9" ry="6" />
                </g>
              </svg>
            </div>
          </div>
          <div className="eclipse-moon" />
        </div>
        <div className="orbital-plane">
          <div className="orbit-track">
            <i />
          </div>
        </div>
        <span className="orbital-label">01 / ECLIPSE SYSTEM</span>
      </div>
      <div className="space-fragments">
        <Fragment className="space-fragment fragment-one" />
        <Fragment className="space-fragment fragment-two" />
        <Fragment className="space-fragment fragment-three" />
      </div>
      <span className="cosmic-coordinate coordinate-top">∞ / НОВАЯ СИСТЕМА КООРДИНАТ</span>
    </CosmicActivity>
  );
}
