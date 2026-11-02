package dev.spaceclient.mods;

import dev.spaceclient.SpaceClient;
import java.time.LocalTime;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class ChatTimestampRuntime {
    private ChatTimestampRuntime() { }
    public static Text decorate(Text original) {
        if(original==null||SpaceClient.mods==null||!SpaceClient.mods.chatTimestamps())return original;
        Text prefix=new LiteralText(ChatTimestamp.prefix(LocalTime.now(),SpaceClient.mods.chatTimeFormat()))
            .setStyle(new Style().setFormatting(Formatting.GRAY));
        // A neutral parent keeps timestamp color separate from message style and links.
        return new LiteralText("").append(prefix).append(original.copy());
    }
}
