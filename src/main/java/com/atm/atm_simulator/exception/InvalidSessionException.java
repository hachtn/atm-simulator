package com.atm.atm_simulator.exception;

public class InvalidSessionException extends RuntimeException {
    public InvalidSessionException() {
        super("Your ATM session is invalid or has expired");
    }
}
