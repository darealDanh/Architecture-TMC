package com.tmc.gate.store;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteNonceRegistryTest {

    @TempDir
    Path dir;

    @Test
    void unusedNonceIsNotUsed() {
        try (LocalDatabase database = LocalDatabase.open(dir.resolve("gate.db"))) {
            assertThat(new SqliteNonceRegistry(database).isUsed("n1")).isFalse();
        }
    }

    @Test
    void markedNonceIsUsedAfterReopen() {
        try (LocalDatabase database = LocalDatabase.open(dir.resolve("gate.db"))) {
            new SqliteNonceRegistry(database).markUsed("n1");
        }
        try (LocalDatabase database = LocalDatabase.open(dir.resolve("gate.db"))) {
            assertThat(new SqliteNonceRegistry(database).isUsed("n1")).isTrue();
        }
    }
}
