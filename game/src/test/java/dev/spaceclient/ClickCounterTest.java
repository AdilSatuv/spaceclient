package dev.spaceclient;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.Test;
import static org.junit.Assert.*;

public class ClickCounterTest {
    @Test public void buttonsAreIndependentAndHoldingDoesNotRepeat() {
        AtomicLong now = new AtomicLong();
        ClickCounter counter = new ClickCounter(now::get);
        counter.onMouseEvent(0, true, true);
        counter.onMouseEvent(0, true, true);
        counter.onMouseEvent(1, true, true);
        counter.onMouseEvent(0, false, true);
        counter.onMouseEvent(0, true, true);
        assertEquals(2, counter.getLeftCps());
        assertEquals(1, counter.getRightCps());
        now.set(2_000_000_000L);
        counter.onMouseEvent(0, true, true);
        assertEquals(0, counter.getLeftCps());
        counter.onMouseEvent(0, false, true);
        counter.onMouseEvent(0, true, true);
        assertEquals(1, counter.getLeftCps());
    }

    @Test public void samplesExpireExactlyAtOneSecondWithoutNewInput() {
        AtomicLong now = new AtomicLong(-500L);
        ClickCounter counter = new ClickCounter(now::get);
        counter.onMouseEvent(0, true, true);
        counter.onMouseEvent(0, false, true);
        now.addAndGet(500_000_000L);
        counter.onMouseEvent(1, true, true);
        now.set(999_999_499L);
        assertEquals(1, counter.getLeftCps());
        now.incrementAndGet();
        assertEquals(0, counter.getLeftCps());
        assertEquals(1, counter.getRightCps());
        now.addAndGet(500_000_000L);
        assertEquals(0, counter.getRightCps());
    }

    @Test public void menuEventsClearHistoryAndCannotCount() {
        AtomicLong now = new AtomicLong();
        ClickCounter counter = new ClickCounter(now::get);
        counter.onMouseEvent(0, true, true);
        counter.onMouseEvent(1, true, false);
        assertEquals(0, counter.getLeftCps());
        assertEquals(0, counter.getRightCps());
        counter.onMouseEvent(0, true, false);
        counter.onMouseEvent(0, true, true);
        assertEquals(1, counter.getLeftCps());
    }

    @Test public void motionWheelMiddleAndReleaseEventsDoNotCount() {
        ClickCounter counter = new ClickCounter(() -> 0L);
        counter.onMouseEvent(-1, false, true);
        counter.onMouseEvent(2, true, true);
        counter.onMouseEvent(0, false, true);
        counter.onMouseEvent(1, false, true);
        assertEquals(0, counter.getLeftCps());
        assertEquals(0, counter.getRightCps());
    }

    @Test public void monotonicNanosecondWrapDoesNotBreakWindow() {
        AtomicLong now = new AtomicLong(Long.MAX_VALUE - 10);
        ClickCounter counter = new ClickCounter(now::get);
        counter.onMouseEvent(0, true, true);
        now.addAndGet(999_999_999L);
        assertEquals(1, counter.getLeftCps());
        now.incrementAndGet();
        assertEquals(0, counter.getLeftCps());
    }
}
