package dev.spaceclient.mods;
/** Bounded mappings of native events. Custom namespaces stay untouched. */
public final class SoundPolicy {
 private SoundPolicy() { }
 public static int group(String id){if(id==null)return 0;String p=id.startsWith("minecraft:")?id.substring(10):id;if(p.indexOf(':')>=0)return 0;if(p.startsWith("dig.")||p.startsWith("step."))return 1;if(p.equals("game.player.hurt")||p.equals("game.hostile.hurt")||(p.startsWith("mob.")&&p.endsWith(".hurt")))return 2;return 0;}
 public static String map(String id,ModSettings.SoundTheme t){int g=group(id);if(g==0||t==ModSettings.SoundTheme.VANILLA)return id;return t==ModSettings.SoundTheme.SOFT?(g==1?"dig.cloth":"random.click"):t==ModSettings.SoundTheme.CRISP?(g==1?"dig.stone":"random.successful_hit"):(g==1?"note.hat":"note.pling");}
 public static float pitch(ModSettings.SoundTheme t){return t==ModSettings.SoundTheme.SOFT?.85f:t==ModSettings.SoundTheme.CRISP?1.15f:t==ModSettings.SoundTheme.ARCADE?1.35f:1;}
}
