package dev.spaceclient.hud;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.spaceclient.SpaceClient;
import dev.spaceclient.settings.ModuleSettingsScreen;
import dev.spaceclient.settings.SettingRow;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.screen.Screen;

/** All HUD modules share profile, layout, appearance and readable native previews. */
public final class HudModuleSettingsScreen extends ModuleSettingsScreen {
    private final HudModule module;
    private int tab;
    public HudModuleSettingsScreen(Screen parent,HudModule module) {
        super(parent);
        if(module==null)throw new IllegalArgumentException("Module is required");
        this.module=module;
    }
    private HudSettings settings() { return SpaceClient.hud; }
    private HudPlacement placement() { return settings().getConfig().active().placement(module); }
    private HudAppearance appearance() { return settings().getConfig().active().appearance(module); }
    private HudContentSettings content() { return settings().getConfig().active().content(); }
    @Override protected String title() { return module.getTitle(); }
    @Override protected String subtitle() { return "Profile: "+settings().getConfig().active().name+"  /  HUD visibility: "+(settings().getConfig().active().enabled?"On":"Off"); }
    @Override protected boolean readOnly() { return settings().isReadOnly(); }
    @Override protected boolean navigation(int id) { return id==5||id==6; }
    @Override protected String notice() {
        if(!settings().getError().isEmpty())return settings().getError();
        if(readOnly())return settings().getNotice();
        if(!status.isEmpty())return status;
        if(tab==1&&appearance().backdrop!=HudAppearance.Backdrop.STRONG)return "Soft / Off backgrounds may reduce readability over bright scenes.";
        return settings().getNotice();
    }
    private boolean contextModule() { return module==HudModule.PING||module==HudModule.BIOME||module==HudModule.PACK_DISPLAY; }
    private boolean hasContentSettings() {
        return module==HudModule.INVENTORY||module==HudModule.COORDINATES||module==HudModule.CLOCK||module==HudModule.FPS||module==HudModule.CPS||module==HudModule.KEYSTROKES||module==HudModule.SPRINT_STATUS||module==HudModule.ARMOR||module==HudModule.POTION_EFFECTS||contextModule();
    }
    @Override protected String[] sections() { return hasContentSettings()?new String[]{"Layout","Appearance","Content"}:new String[]{"Layout","Appearance"}; }
    @Override protected int section() { return tab; }
    @Override protected void section(int value) { tab=value;status=""; }
    @Override protected SettingRow[] footer() { return new SettingRow[]{SettingRow.action(5,"Edit HUD layout","",""),SettingRow.action(6,"Back","","")}; }
    private SettingRow n(int id,String title,String detail,int value,int min,int max) {
        return SettingRow.number(id,title,detail,value,min,max,1,Integer::toString);
    }
    private String moduleDetail() {
        if(module==HudModule.COMBO_COUNTER)return "Client-observed hits; target change, hurt or 2s clears.";
        if(module==HudModule.REACH_DISPLAY)return "Last client hit distance; -- after 5 seconds.";
        if(module==HudModule.SATURATION)return "On servers, ~ marks approximate client saturation.";
        return module.getDescription();
    }
    @Override protected List<SettingRow> rows() {
        List<SettingRow>a=new ArrayList<>();HudContentSettings c=content();EquipmentContent e=c.equipment;
        if(tab==0) {
            a.add(SettingRow.toggle(0,"Enabled",moduleDetail(),module.isEnabled(settings().getConfig())));
            a.add(SettingRow.number(1,"Scale","0.5x to 3.0x; preview fits without changing this value.",(int)Math.round(placement().scale*10),5,30,1,v->String.format(Locale.ROOT,"%.1fx",v/10.0)));
            String anchor=label(placement().vertical.name())+" / "+label(placement().horizontal.name());
            SettingRow ar=SettingRow.choice(3,"Screen anchor","Keeps the widget relative to the selected corner.",anchor);
            a.add(client==null||client.player==null?ar.disabled("Join a world to preserve the widget's screen position."):ar);
            a.add(SettingRow.action(4,"Reset placement","Restore only this widget's scale, position and anchor.","Reset"));
        } else if(tab==1) {
            a.add(SettingRow.choice(8,"Text color","Auto uses this widget's default color.",label(appearance().color.name())));
            a.add(SettingRow.choice(9,"Background","Strong is easiest to read over bright scenes.",label(appearance().backdrop.name())));
            a.add(SettingRow.toggle(10,"Text shadow","Add contrast behind text.",appearance().shadow));
            a.add(SettingRow.action(11,"Reset appearance","Restore color, background and shadow for this widget.","Reset"));
        } else {
            switch(module) {
                case INVENTORY:
                    a.add(SettingRow.toggle(32,"Show hotbar","Include the hotbar below the main inventory grid.",c.inventoryHotbar));
                    a.add(SettingRow.toggle(33,"Item overlays","Show stack counts and durability bars.",c.inventoryOverlays));break;
                case ARMOR:
                    a.add(SettingRow.choice(26,"Display style","Text summary or native item icons.",label(e.armorStyle.name())));
                    a.add(SettingRow.choice(27,"Durability format","Percent remaining or remaining durability points.",label(e.durability.name())));
                    a.add(SettingRow.toggle(28,"Include held item","Show the selected hand item alongside armor.",e.hand));break;
                case POTION_EFFECTS:
                    a.add(SettingRow.choice(29,"Display style","Text summary or native effect icons.",label(e.effectStyle.name())));
                    a.add(n(30,"Visible effects","Show 1 to 5; extra effects appear as +N.",e.maxEffects,1,5));
                    a.add(SettingRow.toggle(31,"Show duration","Display remaining time for each effect.",e.effectTime));break;
                case SPRINT_STATUS:
                    a.add(SettingRow.choice(25,"Display style","Compact separates armed state from actual running.",label(c.sprintDisplay.name())));break;
                case PING:case BIOME:case PACK_DISPLAY:
                    boolean show=module==HudModule.PING?c.pingLabel:module==HudModule.BIOME?c.biomeLabel:c.packLabel;
                    a.add(SettingRow.toggle(22,"Show label","Off leaves just the value or pack name.",show));
                    if(module==HudModule.PACK_DISPLAY)a.add(n(23,"Visible pack names","Remaining resource packs appear as +N.",c.packNames,1,3));break;
                case KEYSTROKES:
                    a.add(SettingRow.toggle(20,"Jump key","Use the current Minecraft jump binding.",c.keystrokesJump));
                    a.add(SettingRow.toggle(21,"Attack and use keys","Show the current attack / use bindings.",c.keystrokesMouse));
                    a.add(SettingRow.choice(24,"Key layout","Choose a compact line or directional blocks.",label(c.keyLayout.name())));break;
                case COORDINATES:
                    a.add(n(14,"Decimal places","0 rounds to whole numbers; 1 or 2 adds precision.",c.coordinatesPrecision,0,2));break;
                case FPS:
                    a.add(SettingRow.toggle(18,"Show FPS label","Off shows only the frame-rate number.",c.fpsLabel));break;
                case CPS:
                    a.add(SettingRow.choice(19,"Mouse buttons","Count left, right or both buttons per second.",label(c.cpsMode.name())));break;
                case CLOCK:
                    a.add(SettingRow.choice(15,"Time format","Uses this computer's local clock.",c.clockFormat==HudContentSettings.ClockFormat.H24?"24 hour":"12 hour"));
                    a.add(SettingRow.toggle(16,"Show seconds","Include seconds in the local time.",c.clockSeconds));break;
                default:break;
            }
            a.add(SettingRow.action(17,"Reset content","Restore only this widget's content options.","Reset"));
        }
        return a;
    }
    @Override protected int previewHeight() { return 82; }
    @Override protected void preview(int x,int y,int w,int h) {
        UiShapes.rounded(x,y,w,h,4,0xFF30343B);
        if(client.player==null) {
            SpaceUiFont.draw(client,"Live preview requires a world",x+12,y+10,MUTED);
            drawLines(moduleDetail(),x+12,y+31,w-24,2,TEXT);return;
        }
        String text=HudRenderer.text(client,module);
        int availableW=w-24,availableH=h-25;
        HudPlacement sample=new HudPlacement(HudPlacement.Horizontal.LEFT,HudPlacement.Vertical.TOP,0,0,previewScale(placement().scale));
        HudPlacement.Bounds b=sample.resolve(availableW,availableH,HudRenderer.moduleWidth(client,module,text),HudRenderer.moduleHeight(client,module));
        SpaceUiFont.draw(client,b.scale<placement().scale?"Live preview / fitted":"Live preview",x+10,y+5,MUTED);
        GlStateManager.pushMatrix();
        try { GlStateManager.translate(x+12,y+22,0);HudRenderer.drawModule(client,module,text,b,appearance(),HudRenderer.color(module,appearance())); }
        finally { GlStateManager.popMatrix(); }
    }
    static double previewScale(double preferred) { return Math.min(1.4,preferred); }
    private static String label(String value) { return value.substring(0,1)+value.substring(1).toLowerCase(Locale.ROOT).replace('_',' '); }
    @Override protected void number(int id,int value) {
        boolean ok;
        if(id==1)ok=settings().place(module,placement().withScale(value/10.0));
        else if(id==14)ok=settings().content(content().withCoordinatesPrecision(value));
        else if(id==23)ok=settings().content(content().withPackNames(value));
        else if(id==30)ok=settings().content(content().withEquipment(content().equipment.maxEffects(value)));
        else return;
        status=ok?"Saved to the active HUD profile.":settings().getError();
    }
    @Override protected void change(int id,int direction) {
        if (id == 6) { client.setScreen(parent); return; }
        if (id == 5) { client.setScreen(new HudEditorScreen(this, module)); return; }
        if (settings().isReadOnly()) return;
        boolean saved;
        if(id==32)saved=settings().content(content().withInventoryHotbar(!content().inventoryHotbar));
        else if(id==33)saved=settings().content(content().withInventoryOverlays(!content().inventoryOverlays));
        else if (id >=26 && id<=31) {
            EquipmentContent e=content().equipment;
            if(id==26)e=e.armorStyle(e.armorStyle==EquipmentContent.Style.TEXT?EquipmentContent.Style.ICONS:EquipmentContent.Style.TEXT);
            else if(id==27)e=e.durability(e.durability==EquipmentContent.Durability.PERCENT?EquipmentContent.Durability.REMAINING:EquipmentContent.Durability.PERCENT);
            else if(id==28)e=e.hand(!e.hand);
            else if(id==29)e=e.effectStyle(e.effectStyle==EquipmentContent.Style.TEXT?EquipmentContent.Style.ICONS:EquipmentContent.Style.TEXT);
            else if(id==30)e=e.maxEffects(Math.max(1,Math.min(5,e.maxEffects+direction)));
            else e=e.effectTime(!e.effectTime);
            saved=settings().content(content().withEquipment(e));
        }
        else if (id == 24) saved = settings().content(content().withKeyLayout(content().keyLayout==HudContentSettings.KeyLayout.LINE ? HudContentSettings.KeyLayout.BLOCK : HudContentSettings.KeyLayout.LINE));
        else if (id == 25) saved = settings().content(content().withSprintDisplay(content().sprintDisplay==HudContentSettings.SprintDisplay.FULL ? HudContentSettings.SprintDisplay.COMPACT : HudContentSettings.SprintDisplay.FULL));
        else if (id == 22) saved = settings().content(module == HudModule.PING ? content().withPingLabel(!content().pingLabel)
            : module == HudModule.BIOME ? content().withBiomeLabel(!content().biomeLabel) : content().withPackLabel(!content().packLabel));
        else if (id == 23) saved = settings().content(content().withPackNames(Math.max(1,Math.min(3,content().packNames+direction))));
        else if (id == 20) saved = settings().content(content().withKeystrokesJump(!content().keystrokesJump));
        else if (id == 21) saved = settings().content(content().withKeystrokesMouse(!content().keystrokesMouse));
        else if (id == 18) saved = settings().content(content().withFpsLabel(!content().fpsLabel));
        else if (id == 19) {
            HudContentSettings c = content();
            HudContentSettings.CpsMode[] values = HudContentSettings.CpsMode.values();
            saved = settings().content(c.withCpsMode(values[Math.floorMod(c.cpsMode.ordinal()+direction,values.length)]));
        } else if (id == 14) {
            HudContentSettings c = content();
            saved = settings().content(c.withCoordinatesPrecision(Math.max(0,Math.min(2,c.coordinatesPrecision+direction))));
        } else if (id == 15) {
            HudContentSettings c = content();
            saved = settings().content(c.withClockFormat(c.clockFormat == HudContentSettings.ClockFormat.H24
                ? HudContentSettings.ClockFormat.H12 : HudContentSettings.ClockFormat.H24));
        } else if (id == 16) saved = settings().content(content().withClockSeconds(!content().clockSeconds));
        else if (id == 17) {
            HudContentSettings c = content();
            if(module==HudModule.INVENTORY)c=c.withInventoryHotbar(false).withInventoryOverlays(true);
            else if (module == HudModule.ARMOR) c=c.withEquipment(c.equipment.armorStyle(EquipmentContent.Style.TEXT).durability(EquipmentContent.Durability.PERCENT).hand(true));
            else if(module==HudModule.POTION_EFFECTS)c=c.withEquipment(c.equipment.effectStyle(EquipmentContent.Style.TEXT).maxEffects(2).effectTime(true));
            else if (module == HudModule.COORDINATES) c = c.withCoordinatesPrecision(1);
            else if (module == HudModule.CLOCK) c = c.withClockFormat(HudContentSettings.ClockFormat.H24).withClockSeconds(true);
            else if (module == HudModule.FPS) c = c.withFpsLabel(true);
            else if (module == HudModule.CPS) c = c.withCpsMode(HudContentSettings.CpsMode.BOTH);
            else if (module == HudModule.KEYSTROKES) c = c.withKeystrokesJump(true).withKeystrokesMouse(true).withKeyLayout(HudContentSettings.KeyLayout.LINE);
            else if (module == HudModule.SPRINT_STATUS) c = c.withSprintDisplay(HudContentSettings.SprintDisplay.FULL);
            else if (module == HudModule.PING) c = c.withPingLabel(true);
            else if (module == HudModule.BIOME) c = c.withBiomeLabel(true);
            else if (module == HudModule.PACK_DISPLAY) c = c.withPackLabel(true).withPackNames(2);
            saved = settings().content(c);
        } else if (id == 8) {
            HudAppearance a = appearance();
            HudAppearance.Color[] values = HudAppearance.Color.values();
            saved = settings().appearance(module, a.withColor(values[Math.floorMod(a.color.ordinal()+direction,values.length)]));
        } else if (id == 9) {
            HudAppearance a = appearance();
            HudAppearance.Backdrop[] values = HudAppearance.Backdrop.values();
            saved = settings().appearance(module, a.withBackdrop(values[Math.floorMod(a.backdrop.ordinal()+direction,values.length)]));
        } else if (id == 10) saved = settings().appearance(module, appearance().withShadow(!appearance().shadow));
        else if (id == 11) saved = settings().appearance(module, HudAppearance.defaults());
        else if (id == 0) saved = settings().toggle(module);
        else if (id == 1 || id == 2) {
            double scale = Math.max(0.5, Math.min(3.0, Math.round((placement().scale + (id == 1 ? -0.1 : 0.1)) * 10) / 10.0));
            saved = settings().place(module, placement().withScale(scale));
        } else if (id == 3) {
            if (client.player == null) return;
            HudPlacement p = placement();
            int next = Math.floorMod(p.horizontal.ordinal()*2+p.vertical.ordinal()+direction,4);
            String text = HudRenderer.text(client, module);
            saved = settings().place(module, p.anchored(HudPlacement.Horizontal.values()[next / 2],
                HudPlacement.Vertical.values()[next % 2], width, height, HudRenderer.moduleWidth(client, module, text), HudRenderer.moduleHeight(client,module)));
        } else if (id == 4) saved = settings().reset(module);
        else return;
        status = saved ? "Saved to the active profile" : settings().getError();
    }
}
