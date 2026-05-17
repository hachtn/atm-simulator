package com.atm.atm_simulator.service.impl;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.atm.atm_simulator.exception.InvalidSessionException;
import com.atm.atm_simulator.model.Account;
import com.atm.atm_simulator.service.AccountService;
import com.atm.atm_simulator.service.AtmSessionService;

@Service
public class AtmSessionServiceImpl implements AtmSessionService {

    private final Map<String, String> sessions = new ConcurrentHashMap<>();
    private final AccountService accountService;

    public AtmSessionServiceImpl(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public String createSession(Account account) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, account.getAccountNumber());
        return token;
    }

    @Override
    public Account getAuthenticatedAccount(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            throw new InvalidSessionException();
        }

        String accountNumber = sessions.get(sessionToken);
        if (accountNumber == null) {
            throw new InvalidSessionException();
        }

        return accountService.getAccountByNumber(accountNumber);
    }

    @Override
    public void closeSession(String sessionToken) {
        if (sessionToken != null && !sessionToken.isBlank()) {
            sessions.remove(sessionToken);
        }
    }
}
