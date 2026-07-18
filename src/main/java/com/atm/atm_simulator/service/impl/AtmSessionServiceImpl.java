package com.atm.atm_simulator.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.atm.atm_simulator.exception.InvalidSessionException;
import com.atm.atm_simulator.model.Account;
import com.atm.atm_simulator.service.AccountService;
import com.atm.atm_simulator.service.AtmSessionService;

@Service
public class AtmSessionServiceImpl implements AtmSessionService {

    private record SessionEntry(String accountNumber, Instant expiresAt) {}

    private final Map<String, SessionEntry> sessions = new ConcurrentHashMap<>();
    private final AccountService accountService;
    private final Duration sessionDuration;

    public AtmSessionServiceImpl(AccountService accountService,
            @Value("${atm.session.timeout-minutes:30}") int timeoutMinutes) {
        this.accountService = accountService;
        this.sessionDuration = Duration.ofMinutes(timeoutMinutes);
    }

    @Override
    public String createSession(Account account) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, new SessionEntry(account.getAccountNumber(), Instant.now().plus(sessionDuration)));
        return token;
    }

    @Override
    public Account getAuthenticatedAccount(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            throw new InvalidSessionException();
        }

        SessionEntry entry = sessions.get(sessionToken);
        if (entry == null || Instant.now().isAfter(entry.expiresAt())) {
            sessions.remove(sessionToken);
            throw new InvalidSessionException();
        }

        return accountService.getAccountByNumber(entry.accountNumber());
    }

    @Override
    public void closeSession(String sessionToken) {
        if (sessionToken != null && !sessionToken.isBlank()) {
            sessions.remove(sessionToken);
        }
    }
}
