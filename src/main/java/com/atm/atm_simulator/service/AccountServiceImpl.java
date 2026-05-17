package com.atm.atm_simulator.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atm.atm_simulator.model.Account;
import com.atm.atm_simulator.model.Transaction;
import com.atm.atm_simulator.model.TransactionType;
import com.atm.atm_simulator.repository.AccountRepository;
import com.atm.atm_simulator.repository.TransactionRepository;

@Service
public class AccountServiceImpl implements AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AccountServiceImpl(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Account getAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
    }

    @Override
    public long getBalance(Long accountId) {
        return getAccount(accountId).getBalance();
    }

    @Override
    @Transactional
    public void deposit(Long accountId, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero");
        }

        Account account = getAccount(accountId);
        account.setBalance(account.getBalance() + amount);
        accountRepository.save(account);

        Transaction tx = new Transaction();
        tx.setType(TransactionType.DEPOSIT);
        tx.setAmount(amount);
        tx.setBalanceAfter(account.getBalance());
        tx.setBranchCode(account.getBranchCode());
        tx.setAccount(account);
        transactionRepository.save(tx);

        log.info("Deposit completed for accountId={}, amount={}, balanceAfter={}",
                accountId, amount, account.getBalance());
    }

    @Override
    @Transactional
    public void withdraw(Long accountId, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero");
        }

        Account account = getAccount(accountId);
        if (account.getBalance() < amount) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        account.setBalance(account.getBalance() - amount);
        accountRepository.save(account);

        Transaction tx = new Transaction();
        tx.setType(TransactionType.WITHDRAW);
        tx.setAmount(amount);
        tx.setBalanceAfter(account.getBalance());
        tx.setBranchCode(account.getBranchCode());
        tx.setAccount(account);
        transactionRepository.save(tx);

        log.info("Withdrawal completed for accountId={}, amount={}, balanceAfter={}",
                accountId, amount, account.getBalance());
    }

    @Override
    public List<Transaction> getTransactionHistory(Long accountId) {
        return transactionRepository.findByAccountIdOrderByTransactionTimeDesc(accountId);
    }
}
