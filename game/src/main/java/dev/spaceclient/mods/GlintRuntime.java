package dev.spaceclient.mods;

import dev.spaceclient.SpaceClient;

public final class GlintRuntime {
    private GlintRuntime(){}
    public static int color(int vanilla){
        ModSettings s=SpaceClient.mods;if(s==null||!s.enchantmentGlint())return vanilla;
        int c=s.glintColor().rgb(),strength=s.glintStrength();
        return 0xFF000000|((c>>16&255)*strength/100<<16)|((c>>8&255)*strength/100<<8)|(c&255)*strength/100;
    }
    public static long time(long vanilla){ModSettings s=SpaceClient.mods;return s!=null&&s.enchantmentGlint()?(long)(vanilla*(s.glintSpeed()/100.0)):vanilla;}
}
