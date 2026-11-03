package dev.spaceclient.cosmetics;

import dev.spaceclient.hud.SpaceUiFont;
import dev.spaceclient.hud.UiShapes;
import dev.spaceclient.settings.ModuleSettingsScreen;
import dev.spaceclient.settings.SettingRow;
import java.util.*;
import net.minecraft.client.gui.screen.Screen;

/** Dedicated cosmetics destination; gameplay/HUD module identities stay unchanged. */
public final class CosmeticsScreen extends ModuleSettingsScreen {
    public CosmeticsScreen(Screen parent) { super(parent); }
    private CosmeticsSettings store() { return CosmeticsRuntime.settings(); }
    private boolean russian() { return client!=null&&"ru_RU".equalsIgnoreCase(client.getLanguageManager().getLanguage().getCode()); }
    private String text(String en,String ru) { return russian()?ru:en; }
    protected String title() { return text("Cosmetics","Косметика"); }
    protected String subtitle() {
        AppearanceProfile profile=CosmeticsRuntime.appearance();
        return profile.linked?text("Space appearance: ","Профиль Space: ")+profile.nickname
            :text("Your local appearance. Set skin and cape in the launcher account.","Локальная внешность. Скин и плащ задаются в аккаунте лаунчера.");
    }
    protected boolean readOnly() { return store().readOnly(); }
    protected String notice() { if(!store().error().isEmpty())return store().error();if(!store().notice().isEmpty())return store().notice();return status; }
    private String wing(CosmeticsSettings.Wings value) {
        switch(value) { case AURORA:return text("Aurora","Сияние");case EMBER:return text("Ember","Пламя");case CRYSTAL:return text("Crystal","Кристалл");default:return text("None","Нет"); }
    }
    private String halo(CosmeticsSettings.Halo value) { return value==CosmeticsSettings.Halo.RING?text("Halo","Нимб"):value==CosmeticsSettings.Halo.ORBIT?text("Orbit","Орбита"):text("None","Нет"); }
    private String tint(CosmeticsSettings.Tint value) { return new String[]{text("Aurora","Сияние"),text("Gold","Золото"),text("Rose","Роза"),text("Ice","Лёд")}[value.ordinal()]; }
    protected List<SettingRow> rows() {
        CosmeticsSettings.State value=store().view();List<SettingRow> rows=new ArrayList<>();
        rows.add(SettingRow.toggle(100,text("Space appearance","Внешность Space"),text("Client visuals for your local player.","Внешность локального игрока в Space."),value.enabled));
        rows.add(SettingRow.choice(101,text("Wings","Крылья"),text("Three shapes; visible from third person.","Три формы; видны от третьего лица."),wing(value.wings)));
        rows.add(SettingRow.choice(102,text("Color palette","Палитра"),text("A shared palette for wings and halo.","Общие цвета крыльев и нимба."),tint(value.tint)));
        rows.add(SettingRow.number(103,text("Wing size","Размер крыльев"),text("Visual size only.","Меняет только внешний вид."),value.scale,60,140,10,n->n+"%"));
        rows.add(SettingRow.toggle(104,text("Animation","Анимация"),text("Gentle movement; off keeps a static pose.","Плавное движение или статичная поза."),value.animated));
        rows.add(SettingRow.choice(105,text("Halo","Нимб"),text("A ring or orbit above the head.","Кольцо или орбита над головой."),halo(value.halo)));
        rows.add(SettingRow.toggle(106,text("Space skin","Скин Space"),text("Use the verified profile skin on the next launch.","Использовать проверенный скин профиля."),value.skin));
        rows.add(SettingRow.toggle(107,text("Space cape","Плащ Space"),text("Show the verified profile cape.","Показывать проверенный плащ профиля."),value.cape));
        if(!value.enabled)for(int i=1;i<rows.size();i++)rows.set(i,rows.get(i).disabled(text("Enable Space appearance first.","Сначала включите внешность Space.")));
        return rows;
    }
    protected void change(int id,int direction) {
        if(id==99) { client.setScreen(parent);return; }
        if(id==98) { store().reset();return; }
        store().change(value->{switch(id) {
            case 100:value.enabled=!value.enabled;break;
            case 101:value.wings=CosmeticsSettings.Wings.values()[Math.floorMod(value.wings.ordinal()+direction,CosmeticsSettings.Wings.values().length)];break;
            case 102:value.tint=CosmeticsSettings.Tint.values()[Math.floorMod(value.tint.ordinal()+direction,CosmeticsSettings.Tint.values().length)];break;
            case 104:value.animated=!value.animated;break;
            case 105:value.halo=CosmeticsSettings.Halo.values()[Math.floorMod(value.halo.ordinal()+direction,CosmeticsSettings.Halo.values().length)];break;
            case 106:value.skin=!value.skin;break;case 107:value.cape=!value.cape;break;
        }});
    }
    protected void number(int id,int value) { if(id==103)store().change(state->state.scale=value); }
}
