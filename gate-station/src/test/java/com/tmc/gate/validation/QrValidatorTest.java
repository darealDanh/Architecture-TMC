package com.tmc.gate.validation;

import static com.tmc.gate.validation.ValidationFixture.NOW;
import static org.assertj.core.api.Assertions.assertThat;

import com.tmc.gate.config.GateSettings;
import com.tmc.gate.credential.ScannedCredential;
import com.tmc.gate.demo.Phone;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class QrValidatorTest {

    private final ValidationFixture fixture = new ValidationFixture();
    private final ValidationFixture.InMemoryNonces nonces = new ValidationFixture.InMemoryNonces();

    @Test
    void validCredentialHasNoViolation() {
        QrValidator validator = QrValidator.standard(GateSettings.defaults(), nonces);

        assertThat(validator.firstViolation(fixture.credential(), fixture.context())).isEmpty();
    }

    @Test
    void returnsFirstViolationInRuleOrder() {
        Phone phone = new Phone("E001", fixture.authority, NOW.minusSeconds(1));
        ScannedCredential credential = fixture.codec.decode(phone.showQr(NOW));
        nonces.markUsed(credential.nonce());
        QrValidator validator = QrValidator.standard(GateSettings.defaults(), nonces);

        assertThat(validator.firstViolation(credential, fixture.context()))
                .contains(RejectionReason.CERTIFICATE_EXPIRED);
    }

    @Test
    void newRuleIsAddedWithoutChangingExistingRules() {
        List<ValidationRule> rules = new ArrayList<>(QrValidator.standard(GateSettings.defaults(), nonces).rules());
        rules.add((credential, context) -> Optional.of(RejectionReason.NOT_AUTHORISED));
        QrValidator validator = new QrValidator(rules);

        assertThat(validator.firstViolation(fixture.credential(), fixture.context()))
                .contains(RejectionReason.NOT_AUTHORISED);
    }
}
