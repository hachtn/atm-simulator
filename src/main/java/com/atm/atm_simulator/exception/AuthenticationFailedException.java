package com.atm.atm_simulator.exception;

public class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException() {
        super("Authentication failed: invalid account number or PIN");
    }
}
