package com.tmc.gate.validation;

public interface NonceRegistry {

    boolean isUsed(String nonce);

    void markUsed(String nonce);
}
