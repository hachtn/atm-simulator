package com.atm.atm_simulator.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.atm.atm_simulator.model.Transaction;
import com.atm.atm_simulator.service.AccountService;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private static final Logger log = LoggerFactory.getLogger(AccountController.class);

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{id}/balance")
    public ResponseEntity<Long> getBalance(@PathVariable Long id) {
        log.info("Balance requested for accountId={}", id);
        return ResponseEntity.ok(accountService.getBalance(id));
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<Void> deposit(@PathVariable Long id, @RequestBody AmountRequest request) {
        log.info("Deposit requested for accountId={}, amount={}", id, request.amount());
        accountService.deposit(id, request.amount());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<Void> withdraw(@PathVariable Long id, @RequestBody AmountRequest request) {
        log.info("Withdrawal requested for accountId={}, amount={}", id, request.amount());
        accountService.withdraw(id, request.amount());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<Transaction>> getTransactions(@PathVariable Long id) {
        log.info("Transaction history requested for accountId={}", id);
        return ResponseEntity.ok(accountService.getTransactionHistory(id));
    }

    record AmountRequest(long amount) {}
}
