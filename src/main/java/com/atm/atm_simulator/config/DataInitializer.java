package com.atm.atm_simulator.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.atm.atm_simulator.model.Account;
import com.atm.atm_simulator.repository.AccountRepository;

@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (accountRepository.count() > 0) {
            return;
        }

        accountRepository.save(buildAccount("0033", "123", "1234567", "Taro Tanaka", "1234", 100_000L));
        accountRepository.save(buildAccount("0033", "456", "7654321", "Hanako Suzuki", "5678", 500_000L));
        accountRepository.save(buildAccount("0033", "789", "9999999", "Jiro Yamada", "0000", 1_000_000L));

        log.info("Sample accounts initialized");
    }

    private Account buildAccount(String bankCode, String branchCode, String accountNumber,
            String holder, String rawPin, long balance) {
        Account account = new Account();
        account.setBankCode(bankCode);
        account.setBranchCode(branchCode);
        account.setAccountNumber(accountNumber);
        account.setAccountHolder(holder);
        account.setPin(passwordEncoder.encode(rawPin));
        account.setBalance(balance);
        return account;
    }
}
