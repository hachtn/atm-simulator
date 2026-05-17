package com.atm.atm_simulator.service;

import java.util.List;

import com.atm.atm_simulator.model.Account;
import com.atm.atm_simulator.model.Transaction;

public interface AccountService {
    Account getAccount(Long accountId);

    long getBalance(Long accountId);

    void deposit(Long accountId, long amount);

    void withdraw(Long accountId, long amount);

    List<Transaction> getTransactionHistory(Long accountId);
}
