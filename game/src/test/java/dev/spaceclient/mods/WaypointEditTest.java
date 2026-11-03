package dev.spaceclient.mods;

import java.nio.file.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class WaypointEditTest {
    private static final String CONTEXT="0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static WaypointStore.Point point() { return new WaypointStore.Point(CONTEXT,"Home",0,-30,64,90,VisualPalette.GOLD,true); }
    private static void clear(WaypointEdit edit) { while(!edit.buffer().isEmpty())edit.backspace(); }
    @Test public void validDraftSavesOnlyTheChosenFieldAndSurvivesReload() throws Exception {
        Path path=Files.createTempDirectory("space-marker-edit").resolve("markers.properties");WaypointStore store=WaypointStore.open(path);WaypointStore.Point p=point();assertTrue(store.add(p));
        WaypointEdit edit=new WaypointEdit(p,1);clear(edit);assertTrue(edit.append("-30000000"));
        assertEquals(-30,store.all().get(0).x);assertTrue(edit.save(store));
        WaypointStore.Point restored=WaypointStore.open(path).all().get(0);
        assertEquals(-30000000,restored.x);assertEquals(p.y,restored.y);assertEquals(p.z,restored.z);
        assertEquals(p.name,restored.name);assertEquals(p.context,restored.context);assertTrue(restored.visible);
    }
    @Test public void invalidCoordinateKeepsDraftAndSavedPointWithAnActionableError() throws Exception {
        Path path=Files.createTempDirectory("space-marker-invalid").resolve("markers.properties");WaypointStore store=WaypointStore.open(path);WaypointStore.Point p=point();store.add(p);byte[] bytes=Files.readAllBytes(path);
        WaypointEdit edit=new WaypointEdit(p,2);clear(edit);assertTrue(edit.append("256"));assertFalse(edit.save(store));
        assertEquals("256",edit.buffer());assertTrue(edit.error().contains("0 to 255"));assertArrayEquals(bytes,Files.readAllBytes(path));
        clear(edit);assertTrue(edit.append("255"));assertTrue(edit.save(store));
    }
    @Test public void nameAndPasteValidationDoNotDiscardTheExistingBuffer() throws Exception {
        WaypointEdit edit=new WaypointEdit(point(),0);assertFalse(edit.append("\n"));assertEquals("Home",edit.buffer());
        clear(edit);assertTrue(edit.append("Метка"));assertTrue(edit.append(" 2"));assertEquals("Метка 2",edit.buffer());
        WaypointEdit coordinate=new WaypointEdit(point(),3);assertFalse(coordinate.append("a"));assertEquals("90",coordinate.buffer());
        clear(coordinate);assertTrue(coordinate.append("-"));assertFalse(coordinate.save(WaypointStore.open(Files.createTempDirectory("space-marker-draft").resolve("markers.properties"))));assertFalse(coordinate.error().isEmpty());
    }
    @Test public void nudgesClampToWorldBoundsAndKeepTheOtherCoordinates() {
        WaypointStore.Point p=point();WaypointStore.Point x=WaypointEdit.nudge(p,1,-10);assertEquals(-40,x.x);assertEquals(64,x.y);assertEquals(90,x.z);
        assertEquals(255,WaypointEdit.nudge(p,2,Integer.MAX_VALUE).y);
        assertEquals(0,WaypointEdit.nudge(p,2,Integer.MIN_VALUE).y);
        assertEquals(30000000,WaypointEdit.nudge(p,3,Integer.MAX_VALUE).z);
        assertEquals(-30000000,WaypointEdit.nudge(p,3,Integer.MIN_VALUE).z);
        assertEquals(-30,p.x);
    }
    @Test public void failedWriteKeepsTheDraftForRetryAndLeavesTheSavedPointUntouched() throws Exception {
        Path parent=Files.createTempDirectory("space-marker-write");Path file=parent.resolve("markers.properties");WaypointStore store=WaypointStore.open(file);WaypointStore.Point p=point();store.add(p);
        Files.delete(file);Files.delete(parent);Files.write(parent,new byte[]{1});
        try {
            WaypointEdit edit=new WaypointEdit(p,0);assertTrue(edit.append(" changed"));assertFalse(edit.save(store));
            assertEquals("Home changed",edit.buffer());assertEquals("Home",store.all().get(0).name);assertTrue(edit.error().contains("reverted"));
        } finally { Files.delete(parent); }
    }
}
