package dev.spaceclient.qa;

import dev.spaceclient.cosmetics.AppearanceProfile;
import java.nio.file.Paths;

/** Pure-JVM end of the real BFF -> main -> public file contract check. */
public final class PublicAppearanceCheck {
    public static void main(String[] args) {
        if(args.length!=3)throw new IllegalArgumentException("config, nickname and model required");
        AppearanceProfile profile=AppearanceProfile.load(Paths.get(args[0]));
        if(!profile.linked||!profile.hasSkin()||!profile.hasCape()||!profile.notice.isEmpty()
            ||!profile.nickname.equals(args[1])||!profile.skinModel.equals(args[2]))
            throw new AssertionError("Exported public appearance was not accepted by the game parser");
        System.out.println("PUBLIC_APPEARANCE_OK");
    }
}
