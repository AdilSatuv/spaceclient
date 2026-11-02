package dev.spaceclient.hud;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.spaceclient.HudConfig;
import dev.spaceclient.SpaceClient;
import dev.spaceclient.mods.SprintRuntime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

/** Shared live data and drawing for the in-game HUD and the editor preview. */
public final class HudRenderer {
    // Keep HUD text legible over bright terrain as well as dark scenes.
    static final int BACKDROP_RGB = 0x10141D;
    static final int BACKDROP_ALPHA = 208;
    static final int HUD_TEXT = 0xFFFFFF;
    static final int HUD_ACCENT = 0xF2B56B;
    static final int HUD_AQUA = 0x80E4ED;
    private HudRenderer() { }
    public static String text(MinecraftClient client, HudModule module) {
        return text(client, module, SpaceClient.hud.getConfig().active().content());
    }
    /** Resolve the same profile-specific text for the live HUD and editor. */
    public static String text(MinecraftClient client, HudModule module, HudContentSettings content) {
        if (content == null) throw new IllegalArgumentException("Missing HUD content settings");
        switch (module) {
            case FPS: return HudMetrics.fps(MinecraftClient.getCurrentFps(), content.fpsLabel);
            case COORDINATES: return client.player == null ? "XYZ --" : HudMetrics.coordinates(
                client.player.x, client.player.y, client.player.z, content.coordinatesPrecision);
            case KEYSTROKES: return KeystrokesHud.text(keys(client,content));
            case CPS: return HudMetrics.cps(SpaceClient.clicks.getLeftCps(), SpaceClient.clicks.getRightCps(), content.cpsMode);
            case COMPASS: return client.player == null ? "Compass --" : "Compass " + Compass.direction(client.player.yaw);
            case CLOCK: return "Time " + HudMetrics.clock(LocalTime.now(), content.clockFormat, content.clockSeconds);
            case SPEED: return client.player == null ? "Speed --" : HudMetrics.speed(
                client.player.x, client.player.z, client.player.prevX, client.player.prevZ, client.isPaused());
            case HEALTH: return client.player == null ? HudVitals.health(Float.NaN, Float.NaN)
                : HudVitals.health(client.player.getHealth(), client.player.getMaxHealth());
            case HUNGER: return client.player == null || client.player.getHungerManager() == null
                ? HudVitals.hunger(null) : HudVitals.hunger(client.player.getHungerManager().getFoodLevel());
            case ARMOR: return ArmorHud.text(client);
            case POTION_EFFECTS: return PotionHud.text(client);
            case MEMORY:
                Runtime vm = Runtime.getRuntime();
                return HudMetrics.memory(vm.totalMemory() - vm.freeMemory(), vm.maxMemory());
            case PLAYTIME: return HudMetrics.playtime(SpaceClient.session.seconds());
            case ITEM_COUNTER: return ItemCounterHud.text(client);
            case PING: return ContextHud.ping(client);
            case BIOME: return ContextHud.biome(client);
            case PACK_DISPLAY: return ContextHud.packs(client);
            case SPRINT_STATUS:
                if (client.player == null) return "ToggleSprint: --";
                boolean enabled = SpaceClient.mods != null && SpaceClient.mods.autoSprint();
                return SprintStatus.text(enabled, enabled && SprintRuntime.state.requested(true, SpaceClient.mods.sprintMode()),
                    client.player.isSprinting());
            default: throw new AssertionError(module);
        }
    }
    private static String bindingName(KeyBinding binding) {
        int code = binding.getCode();
        if(code>=-100&&code<0)return "M"+(code+101);
        return code == 0 ? "Unbound" : GameOptions.getFormattedNameForKeyCode(code);
    }
    private static KeystrokesHud.Key[] keys(MinecraftClient client,HudContentSettings content) {
        List<KeyBinding> bindings=new ArrayList<KeyBinding>();
        GameOptions o=client.options;
        bindings.add(o.forwardKey);bindings.add(o.leftKey);bindings.add(o.backKey);bindings.add(o.rightKey);
        if(content.keystrokesJump)bindings.add(o.jumpKey);
        if(content.keystrokesMouse){bindings.add(o.attackKey);bindings.add(o.useKey);}
        boolean active=client.player!=null&&client.focused&&client.currentScreen==null;
        KeystrokesHud.Key[] result=new KeystrokesHud.Key[bindings.size()];
        for(int i=0;i<result.length;i++){KeyBinding b=bindings.get(i);result[i]=new KeystrokesHud.Key(bindingName(b),active&&b.isPressed());}
        return result;
    }

    public static int textWidth(MinecraftClient client, String text) {
        return Math.max(1, client.textRenderer.getStringWidth(text) + 2);
    }
    public static HudPlacement.Bounds bounds(MinecraftClient client, String text, HudPlacement placement, int width, int height) {
        return placement.resolve(width, height, textWidth(client, text), 10);
    }
    public static int moduleWidth(MinecraftClient client,HudModule module,String text) {
        if(module!=HudModule.KEYSTROKES)return textWidth(client,text);
        int width=2;
        for(KeystrokesHud.Key key:keys(client,SpaceClient.hud.getConfig().active().content()))
            width+=KeystrokesHud.cellWidth(key,client.textRenderer::getStringWidth);
        return width;
    }
    public static HudPlacement.Bounds bounds(MinecraftClient client,HudModule module,String text,HudPlacement placement,int width,int height) {
        return placement.resolve(width,height,moduleWidth(client,module,text),10);
    }
    public static void drawModule(MinecraftClient client,HudModule module,String text,HudPlacement.Bounds bounds,HudAppearance appearance,int color) {
        if(module!=HudModule.KEYSTROKES){draw(client,text,bounds,appearance,color);return;}
        drawKeys(client,keys(client,SpaceClient.hud.getConfig().active().content()),bounds,appearance,color);
    }
    public static void drawKeys(MinecraftClient client,KeystrokesHud.Key[] keys,HudPlacement.Bounds bounds,HudAppearance appearance,int color) {
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(bounds.x,bounds.y,0);GlStateManager.scale(bounds.scale,bounds.scale,1);
            int total=2;for(KeystrokesHud.Key key:keys)total+=KeystrokesHud.cellWidth(key,client.textRenderer::getStringWidth);
            int alpha=backdropAlpha(appearance);if(alpha!=0)backdrop(0,0,total,10,alpha);
            int x=1;
            for(KeystrokesHud.Key key:keys){
                int cell=KeystrokesHud.cellWidth(key,client.textRenderer::getStringWidth);
                String label=key.text();
                client.textRenderer.draw(label,x+(cell-client.textRenderer.getStringWidth(label))/2f,0,color,appearance.shadow);x+=cell;
            }
        }finally{GlStateManager.popMatrix();}
    }
    /** Resolve AUTO against the module so legacy profiles retain their original colors. */
    static int color(HudModule module, HudAppearance appearance) {
        switch (appearance.color) {
            case AUTO: return module == HudModule.KEYSTROKES ? HUD_ACCENT : HUD_TEXT;
            case WHITE: return HUD_TEXT;
            case GOLD: return HUD_ACCENT;
            case AQUA: return HUD_AQUA;
            default: throw new AssertionError(appearance.color);
        }
    }
    static int backdropAlpha(HudAppearance appearance) {
        switch (appearance.backdrop) {
            case STRONG: return BACKDROP_ALPHA;
            case SOFT: return 176;
            case OFF: return 0;
            default: throw new AssertionError(appearance.backdrop);
        }
    }
    /** Color may be overridden by the editor to keep disabled previews visibly muted. */
    public static void draw(MinecraftClient client, String text, HudPlacement.Bounds bounds,
                            HudAppearance appearance, int color) {
        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(bounds.x, bounds.y, 0);
            GlStateManager.scale(bounds.scale, bounds.scale, 1);
            int alpha = backdropAlpha(appearance);
            if (alpha != 0) backdrop(0, 0, textWidth(client, text), 10, alpha);
            client.textRenderer.draw(text, 1f, 0f, color, appearance.shadow);
        } finally { GlStateManager.popMatrix(); }
    }
    public static void render(MinecraftClient client, HudConfig config, int width, int height) {
        if (width < 1 || height < 1) return;
        backdrop(8, 8, 8 + textWidth(client, "SPACE CLIENT"), 18, BACKDROP_ALPHA);
        client.textRenderer.drawWithShadow("SPACE CLIENT", 9, 8, HUD_ACCENT);
        for (HudModule module : HudModule.values()) if (module.isEnabled(config)) {
            String text = text(client, module, config.active().content());
            HudAppearance appearance = config.active().appearance(module);
            drawModule(client, module, text, bounds(client, module, text, config.active().placement(module), width, height),
                appearance, color(module, appearance));
        }
    }

    /** Raw GL with saved attributes leaves GlStateManager's cached state untouched. */
    private static void backdrop(int left, int top, int right, int bottom, int alpha) {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_CURRENT_BIT);
        try {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
            GL11.glColor4f(((BACKDROP_RGB >> 16) & 0xFF) / 255f, ((BACKDROP_RGB >> 8) & 0xFF) / 255f,
                (BACKDROP_RGB & 0xFF) / 255f, alpha / 255f);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glVertex2i(right, top);
            GL11.glVertex2i(left, top);
            GL11.glVertex2i(left, bottom);
            GL11.glVertex2i(right, bottom);
            GL11.glEnd();
        } finally { GL11.glPopAttrib(); }
    }
}
