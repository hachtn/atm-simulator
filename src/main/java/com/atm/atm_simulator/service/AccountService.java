package com.atm.atm_simulator.service;

import java.util.List;

import com.atm.atm_simulator.model.Account;
import com.atm.atm_simulator.model.Transaction;

public interface AccountService {

    Account getAccountByNumber(String accountNumber);

    long getBalance(String accountNumber);

    long deposit(String accountNumber, long amount);

    long withdraw(String accountNumber, long amount);

    long transfer(String accountNumber, String targetAccountNumber, String targetBankName, String note, long amount);

    long topUp(String accountNumber, String target, long amount);

    void changePin(String accountNumber, String currentPin, String newPin);

    List<Transaction> getTransactionHistory(String accountNumber);
}
