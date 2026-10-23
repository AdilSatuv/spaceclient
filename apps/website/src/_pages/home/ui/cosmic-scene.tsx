export function CosmicScene() {
  return (
    <div className="cosmic-scene" aria-hidden="true">
      <div className="starfield starfield-near" />
      <div className="starfield starfield-far" />
      <div className="shooting-star" />
      <div className="orbital-system">
        <div className="orbital-plane">
          <div className="orbit-track">
            <i />
          </div>
        </div>
        <div className="orbital-plane orbital-plane-two">
          <div className="orbit-track">
            <i />
          </div>
        </div>
        <span className="orbital-label">SPACE / БЕЗ ГРАНИЦ</span>
      </div>
    </div>
  );
}
