package dev.spaceclient.hud;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;

/** Compact, single-row effect summary compatible with the existing HUD editor. */
public final class PotionHud {
    private static final int MAX_SHOWN = 2;
    private static final int MAX_NAME_CODEPOINTS = 24;
    private PotionHud() { }

    public static String text(MinecraftClient client) {
        if (client == null || client.player == null) return "Effects --";
        List<Effect> effects = new ArrayList<>();
        for (StatusEffectInstance active : client.player.getStatusEffectInstances()) {
            if (active == null) continue;
            int id = active.getEffectId();
            StatusEffect type = id >= 0 && id < StatusEffect.STATUS_EFFECTS.length
                ? StatusEffect.STATUS_EFFECTS[id] : null;
            String name = type == null ? "Effect " + id : I18n.translate(type.getTranslationKey());
            effects.add(new Effect(id, name, active.getAmplifier(), active.getDuration(), active.isPermanent()));
        }
        return summarize(effects);
    }

    /** A value type independent of Minecraft; one amplifier unit corresponds to level 1. */
    public static final class Effect {
        public final int id, amplifier, ticks;
        public final String name;
        public final boolean permanent;
        public Effect(int id, String name, int amplifier, int ticks, boolean permanent) {
            this.id = id; this.name = name; this.amplifier = amplifier; this.ticks = ticks; this.permanent = permanent;
        }
    }

    public static String summarize(List<Effect> input) {
        if (input == null) return "Effects --";
        List<Effect> effects = new ArrayList<>();
        for (Effect effect : input) if (effect != null) effects.add(effect);
        if (effects.isEmpty()) return "Effects: none";
        effects.sort(Comparator.comparingInt(effect -> effect.id));
        StringBuilder text = new StringBuilder("Effects: ");
        int shown = Math.min(effects.size(), MAX_SHOWN);
        for (int i = 0; i < shown; i++) {
            Effect effect = effects.get(i);
            if (i > 0) text.append(" | ");
            text.append(safeName(effect.name, effect.id)).append(' ')
                .append(effect.amplifier < 0 || effect.amplifier > 255 ? "?" : Integer.toString(effect.amplifier + 1))
                .append(' ').append(duration(effect.ticks, effect.permanent));
        }
        if (effects.size() > shown) text.append(" +").append(effects.size() - shown);
        return text.toString();
    }

    /** Vanilla displays whole seconds; permanent effects use its **:** notation. */
    public static String duration(int ticks, boolean permanent) {
        if (permanent) return "**:**";
        if (ticks < 0) return "--:--";
        int seconds = ticks / 20;
        return (seconds / 60) + ":" + (seconds % 60 < 10 ? "0" : "") + (seconds % 60);
    }

    private static String safeName(String name, int id) {
        if (name == null) return "Effect " + id;
        StringBuilder safe = new StringBuilder();
        name.codePoints().filter(cp -> !Character.isISOControl(cp) && cp != 0x00a7)
            .limit(MAX_NAME_CODEPOINTS).forEach(safe::appendCodePoint);
        String value = safe.toString().trim();
        return value.isEmpty() ? "Effect " + id : value;
    }
}
