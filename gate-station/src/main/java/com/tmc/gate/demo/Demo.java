package com.tmc.gate.demo;

import com.tmc.gate.access.AccessEvent;
import com.tmc.gate.access.AccessResult;
import com.tmc.gate.access.Direction;
import com.tmc.gate.access.GateIdentity;
import com.tmc.gate.access.GateStation;
import com.tmc.gate.access.StartupRecovery;
import com.tmc.gate.alert.Alert;
import com.tmc.gate.alert.Alerts;
import com.tmc.gate.config.GateSettings;
import com.tmc.gate.credential.CredentialCodec;
import com.tmc.gate.store.LocalDatabase;
import com.tmc.gate.store.SqliteAccessEventLog;
import com.tmc.gate.store.SqliteNonceRegistry;
import com.tmc.gate.store.SqliteReferenceData;
import com.tmc.gate.sync.SyncClient;
import com.tmc.gate.sync.SyncReport;
import com.tmc.gate.turnstile.RetryingTurnstileGateway;
import com.tmc.gate.turnstile.SimulatedTurnstileGateway;
import com.tmc.gate.turnstile.UnlockResult;
import com.tmc.gate.validation.QrValidator;
import com.tmc.gate.validation.ReferenceData;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

public final class Demo {

    private static final String FACTORY = "BINH_DUONG";
    private static final GateIdentity GATE = new GateIdentity("BD-GATE-01", FACTORY, Direction.ENTRY);

    private Demo() {
    }

    public static void main(String[] args) {
        run(Path.of("gate-station-demo.db"), System.out);
    }

    static void run(Path databaseFile, PrintStream out) {
        Clock clock = Clock.systemUTC();
        GateSettings settings = GateSettings.defaults();
        ServerAuthority authority = new ServerAuthority();
        InMemoryCentral central = new InMemoryCentral(new ReferenceData(
                authority.publicKey(), Set.of(), Set.of("E002"), Map.of("E001", FACTORY, "E002", FACTORY)));
        Instant certificateExpiry = clock.instant().plusSeconds(7 * 24 * 3600);
        Phone employee = new Phone("E001", authority, certificateExpiry);
        Phone revokedEmployee = new Phone("E002", authority, certificateExpiry);

        try (LocalDatabase database = LocalDatabase.open(databaseFile)) {
            SqliteAccessEventLog events = new SqliteAccessEventLog(database);
            SqliteNonceRegistry nonces = new SqliteNonceRegistry(database);
            SqliteReferenceData referenceData = new SqliteReferenceData(database);
            Alerts alerts = new Alerts();
            GateStation station = new GateStation(GATE, new CredentialCodec(),
                    QrValidator.standard(settings, nonces), referenceData, nonces, events,
                    new RetryingTurnstileGateway(SimulatedTurnstileGateway.sequence(
                            UnlockResult.UNLOCKED, UnlockResult.UNKNOWN, UnlockResult.UNKNOWN,
                            UnlockResult.UNKNOWN, UnlockResult.UNLOCKED), settings.unlockRetries()),
                    alerts, clock);
            SyncClient sync = new SyncClient(GATE, events, referenceData, central, central, central,
                    alerts, settings, clock);

            new StartupRecovery(events, alerts, clock).run();
            printSync(out, "Initial sync", sync.syncOnce());

            String firstCode = employee.showQr(clock.instant());
            printScan(out, "E001 shows a fresh code", station.handleScan(firstCode));
            printScan(out, "Same code scanned again", station.handleScan(firstCode));
            printScan(out, "E001 code from 40 s ago", station.handleScan(employee.showQr(clock.instant().minusSeconds(40))));
            printScan(out, "E002 (revoked) shows a code", station.handleScan(revokedEmployee.showQr(clock.instant())));
            printScan(out, "Turnstile stops answering", station.handleScan(employee.showQr(clock.instant())));

            central.goOffline();
            printSync(out, "Sync while WAN is down", sync.syncOnce());
            printScan(out, "E001 scans while offline", station.handleScan(employee.showQr(clock.instant())));

            central.goOnline();
            printSync(out, "Sync after WAN returns", sync.syncOnce());
            out.printf("central stored %d events, %d still pending at the gate%n",
                    central.storedEventCount(), events.pendingCount());
            for (Alert alert : alerts.active()) {
                out.printf("active alert: %s %s%n", alert.type(), alert.detail());
            }
        }
    }

    private static void printScan(PrintStream out, String label, AccessEvent event) {
        String outcome = event.result() == AccessResult.ACCEPTED
                ? event.unlockStatus().name()
                : event.rejectionReason().name();
        out.printf("%-30s %-9s %s%n", label, event.result(), outcome);
    }

    private static void printSync(PrintStream out, String label, SyncReport report) {
        out.printf("%-30s %-9s uploaded %d%n", label, report.connected() ? "ONLINE" : "OFFLINE", report.uploaded());
    }
}
