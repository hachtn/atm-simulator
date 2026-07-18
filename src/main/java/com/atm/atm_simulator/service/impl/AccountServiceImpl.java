package com.atm.atm_simulator.service.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atm.atm_simulator.exception.AccountNotFoundException;
import com.atm.atm_simulator.exception.AuthenticationFailedException;
import com.atm.atm_simulator.model.Account;
import com.atm.atm_simulator.model.Transaction;
import com.atm.atm_simulator.model.TransactionType;
import com.atm.atm_simulator.repository.AccountRepository;
import com.atm.atm_simulator.repository.TransactionRepository;
import com.atm.atm_simulator.service.AccountService;

@Service
public class AccountServiceImpl implements AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountServiceImpl(AccountRepository accountRepository, TransactionRepository transactionRepository,
            PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public Account getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public long getBalance(String accountNumber) {
        return getAccountByNumber(accountNumber).getBalance();
    }

    @Override
    @Transactional
    public long deposit(String accountNumber, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero");
        }

        Account account = lockAccount(accountNumber);
        account.setBalance(account.getBalance() + amount);
        accountRepository.save(account);

        recordTransaction(account, TransactionType.DEPOSIT, amount, "Cash deposit");

        log.info("Deposit completed for account={}, amount={}, balanceAfter={}", accountNumber, amount, account.getBalance());
        return account.getBalance();
    }

    @Override
    @Transactional
    public long withdraw(String accountNumber, long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero");
        }

        Account account = lockAccount(accountNumber);
        debit(account, amount);
        accountRepository.save(account);

        recordTransaction(account, TransactionType.WITHDRAW, amount, "ATM cash withdrawal");

        log.info("Withdrawal completed for account={}, amount={}, balanceAfter={}", accountNumber, amount, account.getBalance());
        return account.getBalance();
    }

    @Override
    @Transactional
    public long transfer(String accountNumber, String targetAccountNumber, String targetBankName, String note, long amount) {
        if (targetAccountNumber == null || targetAccountNumber.isBlank()) {
            throw new IllegalArgumentException("Target account number is required");
        }
        if (accountNumber.equals(targetAccountNumber)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero");
        }

        // Detect internal transfer before locking to decide the locking strategy.
        boolean isInternal = accountRepository.findByAccountNumber(targetAccountNumber).isPresent();

        Account account;
        Account target = null;

        if (isInternal) {
            // Lock both accounts in consistent lexicographic order to prevent deadlock.
            if (accountNumber.compareTo(targetAccountNumber) < 0) {
                account = lockAccount(accountNumber);
                target = lockAccount(targetAccountNumber);
            } else {
                target = lockAccount(targetAccountNumber);
                account = lockAccount(accountNumber);
            }
        } else {
            account = lockAccount(accountNumber);
        }

        debit(account, amount);
        accountRepository.save(account);

        String description = "Transfer to %s (%s)%s".formatted(
                targetAccountNumber,
                defaultText(targetBankName, "Unknown bank"),
                note == null || note.isBlank() ? "" : " - " + note.trim());
        recordTransaction(account, TransactionType.TRANSFER, amount, description);

        if (target != null) {
            target.setBalance(target.getBalance() + amount);
            accountRepository.save(target);
            recordTransaction(target, TransactionType.TRANSFER_IN, amount,
                    "Transfer received from " + accountNumber);
        }

        log.info("Transfer completed: from={}, to={}, amount={}, internal={}, balanceAfter={}",
                accountNumber, targetAccountNumber, amount, isInternal, account.getBalance());
        return account.getBalance();
    }

    @Override
    @Transactional
    public long topUp(String accountNumber, String target, long amount) {
        if (target == null || target.isBlank()) {
            throw new IllegalArgumentException("Top-up target is required");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Top-up amount must be greater than zero");
        }

        Account account = lockAccount(accountNumber);
        debit(account, amount);
        accountRepository.save(account);

        recordTransaction(account, TransactionType.TOP_UP, amount, "Top-up for " + target.trim());

        log.info("Top-up completed for account={}, target={}, amount={}, balanceAfter={}",
                accountNumber, target, amount, account.getBalance());
        return account.getBalance();
    }

    @Override
    @Transactional
    public void changePin(String accountNumber, String currentPin, String newPin) {
        if (currentPin == null || currentPin.isBlank()) {
            throw new IllegalArgumentException("Current PIN is required");
        }
        if (newPin == null || !newPin.matches("\\d{4}")) {
            throw new IllegalArgumentException("New PIN must be exactly 4 digits");
        }
        if (currentPin.equals(newPin)) {
            throw new IllegalArgumentException("New PIN must be different from the current PIN");
        }

        Account account = lockAccount(accountNumber);
        if (!passwordEncoder.matches(currentPin, account.getPin())) {
            throw new AuthenticationFailedException();
        }

        account.setPin(passwordEncoder.encode(newPin));
        accountRepository.save(account);
        recordTransaction(account, TransactionType.PIN_CHANGE, 0L, "PIN changed");
        log.info("PIN changed for account={}", accountNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transaction> getTransactionHistory(String accountNumber) {
        getAccountByNumber(accountNumber);
        return transactionRepository.findByAccountAccountNumberOrderByTransactionTimeDesc(accountNumber);
    }

    private Account lockAccount(String accountNumber) {
        return accountRepository.findByAccountNumberWithLock(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    private void debit(Account account, long amount) {
        if (account.getBalance() < amount) {
            throw new IllegalArgumentException("Insufficient funds");
        }
        account.setBalance(account.getBalance() - amount);
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private void recordTransaction(Account account, TransactionType type, long amount, String description) {
        Transaction tx = new Transaction();
        tx.setType(type);
        tx.setAmount(amount);
        tx.setBalanceAfter(account.getBalance());
        tx.setBranchCode(account.getBranchCode());
        tx.setDescription(description);
        tx.setAccount(account);
        transactionRepository.save(tx);
    }
}
