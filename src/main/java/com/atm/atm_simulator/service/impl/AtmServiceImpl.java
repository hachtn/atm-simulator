package com.atm.atm_simulator.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.atm.atm_simulator.exception.AuthenticationFailedException;
import com.atm.atm_simulator.model.Account;
import com.atm.atm_simulator.repository.AccountRepository;
import com.atm.atm_simulator.service.AtmService;

@Service
public class AtmServiceImpl implements AtmService {

    // Pre-computed BCrypt hash used as a dummy when the account doesn't exist,
    // so the response time is the same regardless of whether the account was found.
    private static final String DUMMY_HASH = "$2a$10$B1Kz5J3nJ17/9ny4bv9LBeT6FVgX1vMUUyEV0TjZeYaIFPWSu9K/m";

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AtmServiceImpl(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Account authenticate(String accountNumber, String pin) {
        Account account = accountRepository.findByAccountNumber(accountNumber).orElse(null);

        // Always run BCrypt check to prevent timing-based account enumeration.
        String hashToVerify = account != null ? account.getPin() : DUMMY_HASH;
        boolean pinMatches = passwordEncoder.matches(pin, hashToVerify);

        if (account == null || !pinMatches) {
            throw new AuthenticationFailedException();
        }

        return account;
    }
}
