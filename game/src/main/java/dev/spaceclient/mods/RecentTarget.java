package dev.spaceclient.mods;
import java.lang.ref.WeakReference;
/** A target-death cue is separate from server kill-credit attribution. */
public final class RecentTarget {
 private WeakReference<Object> target=new WeakReference<Object>(null);private long attacked;
 public void mark(Object e,long nanos){target=new WeakReference<Object>(e);attacked=nanos;}
 public void clear(){target.clear();}
 public boolean consume(Object e,long nanos){long age=nanos-attacked;if(e==null||target.get()!=e)return false;clear();return age>=0&&age<=3000000000L;}
}
