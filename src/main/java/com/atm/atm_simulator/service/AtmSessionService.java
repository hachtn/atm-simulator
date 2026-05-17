package com.atm.atm_simulator.service;

import com.atm.atm_simulator.model.Account;

public interface AtmSessionService {

    String createSession(Account account);

    Account getAuthenticatedAccount(String sessionToken);

    void closeSession(String sessionToken);
}
