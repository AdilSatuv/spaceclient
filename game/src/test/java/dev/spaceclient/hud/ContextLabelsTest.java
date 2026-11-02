package dev.spaceclient.hud;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public class ContextLabelsTest {
    @Test public void distinguishesSingleplayerUnknownAndServerReportedLatency() {
        assertEquals("Ping: Singleplayer",ContextLabels.ping(true,0));
        assertEquals("Ping: --",ContextLabels.ping(false,null));assertEquals("Ping: --",ContextLabels.ping(false,-1));
        assertEquals("Ping: 0 ms",ContextLabels.ping(false,0));assertEquals("Ping: 148 ms",ContextLabels.ping(false,148));
    }
    @Test public void handlesDefaultMultipleAndServerPacksWithoutUnboundedLabels() {
        assertEquals("Pack: Default",ContextLabels.packs(Collections.emptyList(),false));
        assertEquals("Pack: Server",ContextLabels.packs(Collections.emptyList(),true));
        assertEquals("Packs: Server, A.zip +2",ContextLabels.packs(Arrays.asList("A.zip","B.zip","C.zip"),true));
        assertEquals("Pack: Default",ContextLabels.packs(Arrays.asList(null,"\n"),false));
        assertEquals(24,ContextLabels.clean("abcdefghijklmnopqrstuvwxyz",24).length());
    }
    @Test public void sanitizesBiomeAndResourceNamesWithoutSplittingUnicode() {
        assertEquals("Biome: Plains",ContextLabels.biome(" Plains\n"));
        assertEquals("Biome: --",ContextLabels.biome(null));
        assertEquals("safe",ContextLabels.clean("sa\u202Efe\n\u00a7",32));
        assertEquals("😀😀",ContextLabels.clean("😀😀😀",2));
        assertEquals("Biome: --",ContextHud.biome(null));assertEquals("Pack: --",ContextHud.packs(null));
    }
}
