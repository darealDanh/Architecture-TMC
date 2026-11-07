package com.tmc.gate.access;

public enum RejectionReason {
    MALFORMED,
    INVALID_CERTIFICATE,
    CERTIFICATE_EXPIRED,
    INVALID_SIGNATURE,
    QR_EXPIRED,
    REVOKED,
    NOT_AUTHORISED,
    REPLAYED
}
