package dev.spaceclient.settings;

import dev.spaceclient.hud.HudViewport;
import dev.spaceclient.mods.GameplayModule;
import dev.spaceclient.mods.ModSettings;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import org.junit.Test;
import org.lwjgl.input.Keyboard;
import static org.junit.Assert.*;

/** Exercises the production screen input handlers without creating a window or LWJGL device. */
public class SettingsInputTest {
    private static final class Harness extends ModuleSettingsScreen {
        final ModSettings settings;
        boolean shift,repeated,back,shrinkOnReset,empty;
        int writes;
        Harness(Path path) { super(null);settings=ModSettings.open(path);width=640;height=360;init(); }
        protected String title() { return "Input test"; }
        protected String subtitle() { return ""; }
        protected boolean readOnly() { return settings.isReadOnly(); }
        protected String notice() { return settings.error(); }
        protected boolean navigation(int id) { return id==99; }
        protected boolean repeatedInput() { return repeated; }
        protected boolean shiftInput() { return shift; }
        protected List<SettingRow> rows() {
            List<SettingRow> rows=new ArrayList<>();
            if(empty)return rows;
            rows.add(SettingRow.number(42,"Size","Effect",settings.heldItemScale(),50,150,10,v->v+"%"));
            rows.add(SettingRow.toggle(0,"Enabled","Effect",settings.itemSize()));
            for(int i=0;i<10;i++)rows.add(SettingRow.choice(100+i,"Choice","Effect","Value"));
            rows.add(SettingRow.number(43,"Unavailable","Effect",settings.droppedItemScale(),25,200,25,Integer::toString).disabled("Choose another mode"));
            return rows;
        }
        protected void number(int id,int value) { writes++;settings.heldItemScale(value); }
        protected void change(int id,int direction) {
            if(id==0)settings.toggleItemSize();
            if(id==98) { settings.reset(GameplayModule.ITEM_SIZE);if(shrinkOnReset)empty=true; }
            if(id==99)back=true;
        }
        void key(int key) { keyPressed((char)0,key); }
        int px(int ui) { return Math.round(ui*new HudViewport(width,height).scale); }
        void click(int x,int y) { mouseClicked(px(x),px(y),0); }
        void drag(int x,int y) { mouseDragged(px(x),px(y),0,1); }
        void release(int x,int y) { mouseReleased(px(x),px(y),0); }
        String help(int id) { for(SettingRow row:rows())if(row.id==id)return rowHelp(row);throw new AssertionError(id); }
    }
    @Test public void keyboardAdjustmentsPersistAndDoNotWriteAgainAtTheLimits() throws Exception {
        Path path=Files.createTempDirectory("space-input").resolve("mods.properties");Harness s=new Harness(path);
        s.key(Keyboard.KEY_RIGHT);assertEquals(90,s.settings.heldItemScale());
        s.shift=true;s.key(Keyboard.KEY_LEFT);assertEquals(50,s.settings.heldItemScale());
        s.key(Keyboard.KEY_LEFT);assertEquals(2,s.writes);
        s.key(Keyboard.KEY_END);s.key(Keyboard.KEY_END);s.key(Keyboard.KEY_RIGHT);
        assertEquals(3,s.writes);assertEquals(150,ModSettings.open(path).heldItemScale());
    }
    @Test public void mouseDragHasOneCommitOnReleaseAndEscapeRestoresTheSavedValue() throws Exception {
        Path path=Files.createTempDirectory("space-drag").resolve("mods.properties");Harness s=new Harness(path);
        s.settings.heldItemScale(83);byte[] original=Files.readAllBytes(path);
        int x=s.layout.trackX(),y=s.layout.bodyTop+28,w=s.layout.trackWidth();
        s.click(x+w/2,y);s.drag(x+w,y);
        assertEquals(0,s.writes);assertEquals(83,s.settings.heldItemScale());assertArrayEquals(original,Files.readAllBytes(path));
        s.key(Keyboard.KEY_ESCAPE);s.release(x+w,y);assertEquals(0,s.writes);
        s.click(x+w/2,y);s.drag(x+w+100,y);s.release(x+w+100,y);
        assertEquals(1,s.writes);assertEquals(150,ModSettings.open(path).heldItemScale());
    }
    @Test public void mouseMinusAndPlusUseTheSameNonWrappingAdjustmentAsKeys() throws Exception {
        Harness s=new Harness(Files.createTempDirectory("space-step").resolve("mods.properties"));
        int x=s.layout.controlX(),y=s.layout.bodyTop+15;
        s.click(x+10,y);assertEquals(70,s.settings.heldItemScale());
        s.click(x+181,y);assertEquals(80,s.settings.heldItemScale());
        s.settings.heldItemScale(150);s.click(x+181,y);assertEquals(2,s.writes);
    }
    @Test public void tabShiftTabAndPageKeysRevealFocusWithoutActivatingRows() throws Exception {
        Harness s=new Harness(Files.createTempDirectory("space-nav").resolve("mods.properties"));
        for(int i=0;i<11;i++)s.key(Keyboard.KEY_TAB);
        assertEquals(0,s.writes);assertFalse(s.settings.itemSize());
        s.key(Keyboard.KEY_PRIOR);s.key(Keyboard.KEY_NEXT);s.shift=true;s.key(Keyboard.KEY_TAB);
        s.key(Keyboard.KEY_END); // Disabled numeric row or footer must not mutate Size.
        assertEquals(80,s.settings.heldItemScale());
        s.shift=false;
        for(int i=0;i<40&&!s.back;i++) { s.key(Keyboard.KEY_TAB);s.key(Keyboard.KEY_RETURN); }
        assertTrue(s.back);
    }
    @Test public void readonlyAndDependentDisabledRowsIgnoreBothKeyboardAndPointer() throws Exception {
        Path path=Files.createTempFile("space-readonly-input",".properties");byte[] bytes="schemaVersion=999\n".getBytes(StandardCharsets.UTF_8);Files.write(path,bytes);
        Harness locked=new Harness(path);locked.key(Keyboard.KEY_END);locked.click(locked.layout.controlX()+180,locked.layout.bodyTop+15);
        assertEquals(0,locked.writes);assertArrayEquals(bytes,Files.readAllBytes(path));
        Harness s=new Harness(Files.createTempDirectory("space-disabled-input").resolve("mods.properties"));
        for(int i=0;i<12;i++)s.key(Keyboard.KEY_TAB);
        s.key(Keyboard.KEY_RIGHT);s.key(Keyboard.KEY_END);assertEquals(0,s.writes);assertEquals(100,s.settings.droppedItemScale());
    }
    @Test public void repeatEventsDoNotToggleBooleanRowsRepeatedly() throws Exception {
        Harness s=new Harness(Files.createTempDirectory("space-repeat-input").resolve("mods.properties"));
        s.key(Keyboard.KEY_TAB);s.key(Keyboard.KEY_SPACE);assertTrue(s.settings.itemSize());
        s.repeated=true;s.key(Keyboard.KEY_SPACE);assertTrue(s.settings.itemSize());
    }
    @Test public void changingTheNumberOfRowsKeepsFocusInsideTheNewControlList() throws Exception {
        Harness s=new Harness(Files.createTempDirectory("space-shrinking-input").resolve("mods.properties"));s.shrinkOnReset=true;
        for(int i=0;i<13;i++)s.key(Keyboard.KEY_TAB);
        s.key(Keyboard.KEY_RETURN);assertTrue(s.empty);
        s.key(Keyboard.KEY_RETURN);assertTrue(s.back);
    }
    @Test public void arrowsOnToggleAndFooterDoNotActivateActions() throws Exception {
        Harness s=new Harness(Files.createTempDirectory("space-actions").resolve("mods.properties"));
        s.key(Keyboard.KEY_TAB);s.key(Keyboard.KEY_RIGHT);s.key(Keyboard.KEY_LEFT);
        assertFalse(s.settings.itemSize());
        for(int i=0;i<13;i++)s.key(Keyboard.KEY_TAB);
        s.key(Keyboard.KEY_RIGHT);s.key(Keyboard.KEY_LEFT);assertFalse(s.back);
        s.key(Keyboard.KEY_RETURN);assertTrue(s.back);
    }
    @Test public void disabledHelpExplainsTheReasonWithoutInvitingAdjustment() throws Exception {
        Harness s=new Harness(Files.createTempDirectory("space-help").resolve("mods.properties"));
        String help=s.help(43);assertTrue(help.contains("Unavailable"));assertTrue(help.contains("Choose another mode"));
        assertFalse(help.contains("Left / Right"));assertFalse(help.contains("Enter"));
        assertTrue(s.help(42).contains("Home / End"));
        Path p=Files.createTempFile("space-locked-help",".properties");Files.write(p,"schemaVersion=999\n".getBytes(StandardCharsets.UTF_8));
        assertTrue(new Harness(p).help(42).contains("Read-only"));
    }
}
