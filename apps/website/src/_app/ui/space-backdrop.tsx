export function SpaceBackdrop() {
  return (
    <div className="space-backdrop" aria-hidden="true">
      <div className="backdrop-nebula" />
      <div className="starfield starfield-far" />
      <div className="starfield starfield-near" />
      <div className="meteor-field">
        <span className="asteroid-drift asteroid-one">
          <i />
        </span>
        <span className="asteroid-drift asteroid-two">
          <i />
        </span>
        <span className="asteroid-drift asteroid-three">
          <i />
        </span>
        <span className="asteroid-drift asteroid-four">
          <i />
        </span>
        <span className="distant-comet" />
      </div>
      <div className="backdrop-grid" />
    </div>
  );
}
