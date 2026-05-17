package com.atm.atm_simulator.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.atm.atm_simulator.model.Account;
import com.atm.atm_simulator.model.Transaction;
import com.atm.atm_simulator.service.AccountService;
import com.atm.atm_simulator.service.AtmService;
import com.atm.atm_simulator.service.AtmSessionService;

@RestController
@RequestMapping("/api/atm")
public class AtmSessionController {

    private static final Logger log = LoggerFactory.getLogger(AtmSessionController.class);

    private final AtmService atmService;
    private final AtmSessionService atmSessionService;
    private final AccountService accountService;

    public AtmSessionController(AtmService atmService, AtmSessionService atmSessionService, AccountService accountService) {
        this.atmService = atmService;
        this.atmSessionService = atmSessionService;
        this.accountService = accountService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        Account account = atmService.authenticate(request.accountNumber(), request.pin());
        String token = atmSessionService.createSession(account);
        log.info("ATM login succeeded for account={}", account.getAccountNumber());
        return ResponseEntity.ok(new LoginResponse(token, toSummary(account)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        atmSessionService.closeSession(extractToken(authorizationHeader));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/account")
    public ResponseEntity<AccountSummary> getAccount(@RequestHeader("Authorization") String authorizationHeader) {
        Account account = requireAccount(authorizationHeader);
        return ResponseEntity.ok(toSummary(account));
    }

    @GetMapping("/balance")
    public ResponseEntity<BalanceResponse> getBalance(@RequestHeader("Authorization") String authorizationHeader) {
        Account account = requireAccount(authorizationHeader);
        long balance = accountService.getBalance(account.getAccountNumber());
        return ResponseEntity.ok(new BalanceResponse(balance));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<BalanceResponse> withdraw(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody AmountRequest request) {
        Account account = requireAccount(authorizationHeader);
        long balance = accountService.withdraw(account.getAccountNumber(), request.amount());
        return ResponseEntity.ok(new BalanceResponse(balance));
    }

    @PostMapping("/transfer")
    public ResponseEntity<BalanceResponse> transfer(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody TransferRequest request) {
        Account account = requireAccount(authorizationHeader);
        long balance = accountService.transfer(
                account.getAccountNumber(),
                request.targetAccountNumber(),
                request.targetBankName(),
                request.note(),
                request.amount());
        return ResponseEntity.ok(new BalanceResponse(balance));
    }

    @PostMapping("/topup")
    public ResponseEntity<BalanceResponse> topUp(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody TopUpRequest request) {
        Account account = requireAccount(authorizationHeader);
        long balance = accountService.topUp(account.getAccountNumber(), request.target(), request.amount());
        return ResponseEntity.ok(new BalanceResponse(balance));
    }

    @PostMapping("/change-pin")
    public ResponseEntity<MessageResponse> changePin(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody ChangePinRequest request) {
        Account account = requireAccount(authorizationHeader);
        accountService.changePin(account.getAccountNumber(), request.currentPin(), request.newPin());
        return ResponseEntity.ok(new MessageResponse("PIN changed successfully"));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getTransactions(@RequestHeader("Authorization") String authorizationHeader) {
        Account account = requireAccount(authorizationHeader);
        return ResponseEntity.ok(accountService.getTransactionHistory(account.getAccountNumber()));
    }

    private Account requireAccount(String authorizationHeader) {
        return atmSessionService.getAuthenticatedAccount(extractToken(authorizationHeader));
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader == null) {
            return null;
        }
        if (authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7).trim();
        }
        return authorizationHeader.trim();
    }

    private AccountSummary toSummary(Account account) {
        return new AccountSummary(
                account.getBankCode(),
                account.getBranchCode(),
                account.getAccountNumber(),
                account.getAccountHolder(),
                account.getBalance());
    }

    record LoginRequest(String accountNumber, String pin) {}

    record LoginResponse(String token, AccountSummary account) {}

    record AccountSummary(String bankCode, String branchCode, String accountNumber, String accountHolder, long balance) {}

    record BalanceResponse(long balance) {}

    record AmountRequest(long amount) {}

    record TransferRequest(String targetAccountNumber, String targetBankName, String note, long amount) {}

    record TopUpRequest(String target, long amount) {}

    record ChangePinRequest(String currentPin, String newPin) {}

    record MessageResponse(String message) {}
}
