package com.tmc.gate.credential;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.regex.Pattern;

public final class CredentialCodec {

    private static final String VERSION = "v1";
    private static final int SEGMENT_COUNT = 9;
    private static final Pattern SEPARATOR = Pattern.compile(Pattern.quote(Segments.SEPARATOR));

    public String encode(ScannedCredential credential) {
        return VERSION + Segments.SEPARATOR
                + String.join(Segments.SEPARATOR, credential.contentSegments())
                + Segments.SEPARATOR + Segments.ofBytes(credential.deviceSignature());
    }

    public ScannedCredential decode(String raw) {
        String[] segments = SEPARATOR.split(raw == null ? "" : raw, -1);
        if (segments.length != SEGMENT_COUNT || !VERSION.equals(segments[0])) {
            throw new MalformedCredentialException("Unrecognised QR format");
        }
        try {
            return new ScannedCredential(
                    decodeCertificate(segments),
                    Instant.ofEpochMilli(Long.parseLong(Segments.toText(segments[6]))),
                    requireEncoded(segments[7]),
                    Segments.toBytes(segments[8]));
        } catch (IllegalArgumentException | DateTimeException e) {
            throw new MalformedCredentialException("Unreadable QR content", e);
        }
    }

    private static DeviceCertificate decodeCertificate(String[] segments) {
        return new DeviceCertificate(
                Segments.toText(segments[1]),
                Segments.toText(segments[2]),
                Ed25519.decodePublicKey(Segments.toBytes(segments[3])),
                Instant.ofEpochSecond(Long.parseLong(Segments.toText(segments[4]))),
                Segments.toBytes(segments[5]));
    }

    private static String requireEncoded(String segment) {
        Segments.toBytes(segment);
        return segment;
    }
}
