package dev.spaceclient.mods;
/** Cosmetic motion, without player attack or animation packets. */
public final class SwingClock {
 private boolean started;private long start;
 public void begin(long nanos){start=nanos;started=true;}
 public float progress(long nanos){long age=nanos-start;return !started||age<0||age>=250000000L?0:(age+1)/250000000f;}
}
