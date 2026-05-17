package com.atm.atm_simulator.service;

import com.atm.atm_simulator.model.Account;

public interface AtmService {
    Account authenticate(String accountNumber, String pin);
}
