package com.atm.atm_simulator.service;

import org.springframework.stereotype.Service;

import com.atm.atm_simulator.model.Account;
import com.atm.atm_simulator.repository.AccountRepository;

@Service
public class AtmServiceImpl implements AtmService {

    private final AccountRepository accountRepository;

    public AtmServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    public Account authenticate(String accountNumber, String pin) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getPin().equals(pin)) {
            throw new RuntimeException("Incorrect PIN");
        }

        return account;
    }
}
