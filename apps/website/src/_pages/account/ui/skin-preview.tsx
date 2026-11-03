'use client';
import { useEffect, useRef } from 'react';
export default function SkinPreview({ src, slim }: { src: string; slim: boolean }) {
  const canvas = useRef<HTMLCanvasElement>(null);
  useEffect(() => {
    const target = canvas.current;
    if (!target) return;
    const context = target.getContext('2d');
    if (!context) return;
    context.clearRect(0, 0, 160, 328);
    if (!src) return;
    let active = true;
    const image = new Image();
    image.onload = () => {
      if (!active) return;
      context.imageSmoothingEnabled = false;
      const draw = (
        x: number,
        y: number,
        w: number,
        h: number,
        dx: number,
        dy: number,
        dw: number,
        dh: number,
        mirror = false,
      ) => {
        if (!mirror) {
          context.drawImage(image, x, y, w, h, dx, dy, dw, dh);
          return;
        }
        context.save();
        context.translate(dx + dw, dy);
        context.scale(-1, 1);
        context.drawImage(image, x, y, w, h, 0, 0, dw, dh);
        context.restore();
      };
      draw(8, 8, 8, 8, 40, 0, 80, 80);
      draw(20, 20, 8, 12, 40, 80, 80, 120);
      const modern = image.height === 64;
      const arm = modern && slim ? 3 : 4,
        armWidth = arm * 10;
      draw(44, 20, arm, 12, 40 - armWidth, 80, armWidth, 120);
      draw(modern ? 36 : 44, modern ? 52 : 20, arm, 12, 120, 80, armWidth, 120, !modern);
      draw(4, 20, 4, 12, 40, 200, 40, 120);
      draw(modern ? 20 : 4, modern ? 52 : 20, 4, 12, 80, 200, 40, 120, !modern);
      if (modern) {
        draw(20, 36, 8, 12, 40, 80, 80, 120); // Jacket.
        draw(44, 36, arm, 12, 40 - armWidth, 80, armWidth, 120);
        draw(52, 52, arm, 12, 120, 80, armWidth, 120);
        draw(4, 36, 4, 12, 40, 200, 40, 120);
        draw(4, 52, 4, 12, 80, 200, 40, 120);
      }
      draw(40, 8, 8, 8, 40, 0, 80, 80);
    };
    image.src = src;
    return () => {
      active = false;
    };
  }, [src, slim]);
  return (
    <canvas
      ref={canvas}
      width={160}
      height={328}
      className="skin-figure"
      role="img"
      aria-label="Предпросмотр скина спереди, 2D"
    />
  );
}
