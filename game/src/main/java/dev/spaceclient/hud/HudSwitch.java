package dev.spaceclient.hud;

/** Smooth pill and thumb; each screen owns animation state, while hitboxes stay fixed. */
public final class HudSwitch {
    private HudSwitch() { }
    public static void draw(int x, int y, boolean enabled) { draw(x,y,enabled ? 1 : 0); }
    public static void draw(int x, int y, float amount) {
        float t = Math.max(0,Math.min(1,amount));
        UiShapes.rounded(x+2,y+1,24,12,6,mix(0xFF3A3C40,0xFFD7AF6A,t));
        UiShapes.rounded(x+4+11*t,y+3,10,10,5,0x40000000);
        UiShapes.rounded(x+4+11*t,y+2,10,10,5,mix(0xFF979A9E,0xFFF4F1EA,t));
    }
    private static int mix(int a, int b, float t) {
        int r=0xFF000000;
        for(int shift=0;shift<=16;shift+=8) r |= Math.round(((a>>shift)&255)*(1-t)+((b>>shift)&255)*t)<<shift;
        return r;
    }
    public static final class Motion {
        private float amount;
        private long previous;
        public Motion(boolean enabled) { amount=enabled ? 1 : 0; }
        public float sample(boolean enabled,long nanos) {
            float target=enabled ? 1 : 0;
            if(previous!=0) {
                double dt=Math.max(0,Math.min(.1,(nanos-previous)/1_000_000_000.0));
                amount+=(target-amount)*(1-Math.exp(-dt*24));
                if(Math.abs(target-amount)<.001)amount=target;
            }
            previous=nanos;
            return amount;
        }
    }
}
