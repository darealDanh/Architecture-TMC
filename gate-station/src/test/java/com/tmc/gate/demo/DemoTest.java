package com.tmc.gate.demo;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DemoTest {

    @TempDir
    Path dir;

    @Test
    void runsEndToEnd() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        Demo.run(dir.resolve("demo.db"), new PrintStream(buffer, true, StandardCharsets.UTF_8));

        String output = buffer.toString(StandardCharsets.UTF_8);
        assertThat(output)
                .contains("UNLOCKED")
                .contains("REPLAYED")
                .contains("QR_EXPIRED")
                .contains("REVOKED")
                .contains("UNLOCK_UNKNOWN")
                .contains("central stored 6 events");
    }
}
