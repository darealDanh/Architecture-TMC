package com.tmc.gate.store;

import static org.assertj.core.api.Assertions.assertThat;

import com.tmc.gate.demo.ServerAuthority;
import com.tmc.gate.validation.ReferenceData;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteReferenceDataTest {

    @TempDir
    Path dir;

    private LocalDatabase database;
    private SqliteReferenceData store;

    @BeforeEach
    void open() {
        database = LocalDatabase.open(dir.resolve("gate.db"));
        store = new SqliteReferenceData(database);
    }

    @AfterEach
    void close() {
        database.close();
    }

    @Test
    void emptyBeforeFirstDownload() {
        assertThat(store.current()).isEqualTo(ReferenceData.empty());
    }

    @Test
    void replaceStoresAndReturnsAllFields() {
        ServerAuthority authority = new ServerAuthority();
        ReferenceData data = new ReferenceData(authority.publicKey(),
                Set.of("serial-1"), Set.of("E009"), Map.of("E001", "BINH_DUONG", "E002", "LONG_AN"));

        store.replace(data);
        ReferenceData stored = store.current();

        assertThat(stored.serverPublicKey().getEncoded()).isEqualTo(authority.publicKey().getEncoded());
        assertThat(stored.revokedCertificateSerials()).containsExactly("serial-1");
        assertThat(stored.revokedEmployeeIds()).containsExactly("E009");
        assertThat(stored.activeEmployeeFactories())
                .containsExactlyInAnyOrderEntriesOf(Map.of("E001", "BINH_DUONG", "E002", "LONG_AN"));
    }

    @Test
    void replaceOverwritesPreviousData() {
        ServerAuthority authority = new ServerAuthority();
        store.replace(new ReferenceData(authority.publicKey(), Set.of("old"), Set.of("E009"), Map.of("E001", "BINH_DUONG")));

        store.replace(new ReferenceData(authority.publicKey(), Set.of(), Set.of(), Map.of("E002", "LONG_AN")));
        ReferenceData stored = store.current();

        assertThat(stored.revokedCertificateSerials()).isEmpty();
        assertThat(stored.revokedEmployeeIds()).isEmpty();
        assertThat(stored.activeEmployeeFactories()).containsExactlyEntriesOf(Map.of("E002", "LONG_AN"));
    }
}
