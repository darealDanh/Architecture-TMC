package com.tmc.gate.credential;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

final class Segments {

    static final String SEPARATOR = ".";

    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private Segments() {
    }

    static String ofBytes(byte[] value) {
        return ENCODER.encodeToString(value);
    }

    static String ofText(String value) {
        return ofBytes(value.getBytes(StandardCharsets.UTF_8));
    }

    static byte[] toBytes(String segment) {
        return DECODER.decode(segment);
    }

    static String toText(String segment) {
        return new String(toBytes(segment), StandardCharsets.UTF_8);
    }

    static byte[] join(String... segments) {
        return String.join(SEPARATOR, segments).getBytes(StandardCharsets.UTF_8);
    }
}
