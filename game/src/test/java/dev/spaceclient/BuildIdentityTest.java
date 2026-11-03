package dev.spaceclient;

import java.util.Properties;
import org.junit.Test;
import static org.junit.Assert.*;

public class BuildIdentityTest {
    private Properties stamp(String tree) {
        Properties p=new Properties();
        p.setProperty("schemaVersion","1");p.setProperty("channel","development");
        p.setProperty("gameSourceTree",tree);return p;
    }
    @Test public void labelUsesOnlyTheCommittedTreePrefix() {
        Properties p=stamp("1234567890abcdef1234567890abcdef12345678");
        p.setProperty("modVersion","9.9.9");p.setProperty("timestamp","untrusted");
        assertEquals("dev-1234567890ab",BuildIdentity.fromProperties(p));
    }
    @Test public void missingMalformedAndUnsupportedStampsHaveAnExplicitUnknownLabel() {
        assertEquals("dev-unknown",BuildIdentity.fromProperties(null));
        assertEquals("dev-unknown",BuildIdentity.fromProperties(new Properties()));
        for(String tree:new String[]{"","1234567890ab","g234567890abcdef1234567890abcdef12345678"," 1234567890abcdef1234567890abcdef12345678"})
            assertEquals("dev-unknown",BuildIdentity.fromProperties(stamp(tree)));
        Properties p=stamp("1234567890abcdef1234567890abcdef12345678");
        p.setProperty("schemaVersion","2");assertEquals("dev-unknown",BuildIdentity.fromProperties(p));
        p.setProperty("schemaVersion","1");p.setProperty("channel","release");
        assertEquals("dev-unknown",BuildIdentity.fromProperties(p));
    }
}
