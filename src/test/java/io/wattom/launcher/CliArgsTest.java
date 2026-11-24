package io.wattom.launcher;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CliArgsTest {

    @Test
    void parsesRequiredAndOptionalFlags() {
        CliArgs args = CliArgs.parse(new String[]{
            "--pid", "12345", "--trace", "com.example.Foo#bar", "--refresh", "250"
        });
        assertEquals("12345", args.pid());
        assertEquals("com.example.Foo#bar", args.traceSpec());
        assertEquals(250, args.refreshMs());
        assertFalse(args.help());
    }

    @Test
    void parsesHelpFlagWithoutPid() {
        CliArgs args = CliArgs.parse(new String[]{"--help"});
        assertTrue(args.help());
    }

    @Test
    void rejectsMissingPidAndUnknownFlags() {
        assertThrows(IllegalArgumentException.class, () -> CliArgs.parse(new String[]{}));
        assertThrows(IllegalArgumentException.class, () -> CliArgs.parse(new String[]{"--pid"}));
        assertThrows(IllegalArgumentException.class, () -> CliArgs.parse(new String[]{"--bogus"}));
        assertThrows(IllegalArgumentException.class, () -> CliArgs.parse(new String[]{"--pid", "1", "--refresh", "0"}));
    }
}
