package com.atm.atm_simulator.simulator;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.atm.atm_simulator.model.Account;
import com.atm.atm_simulator.model.Transaction;
import com.atm.atm_simulator.service.AccountService;
import com.atm.atm_simulator.service.AtmService;

@Component
@ConditionalOnProperty(name = "atm.simulator.cli.enabled", havingValue = "true")
public class AtmSimulator implements CommandLineRunner {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AtmService atmService;
    private final AccountService accountService;
    private final Scanner scanner = new Scanner(System.in);

    public AtmSimulator(AtmService atmService, AccountService accountService) {
        this.atmService = atmService;
        this.accountService = accountService;
    }

    @Override
    public void run(String... args) {
        printBanner();
        while (true) {
            Account account = authenticate();
            if (account != null) {
                runSession(account);
            }
        }
    }

    private void printBanner() {
        System.out.println();
        System.out.println("╔══════════════════════════════════╗");
        System.out.println("║     ATM SIMULATOR - MUFG Bank    ║");
        System.out.println("╚══════════════════════════════════╝");
    }

    private Account authenticate() {
        System.out.println();
        System.out.print("口座番号 (Account Number): ");
        String accountNumber = scanner.nextLine().trim();
        System.out.print("暗証番号 (PIN)           : ");
        String pin = scanner.nextLine().trim();

        try {
            Account account = atmService.authenticate(accountNumber, pin);
            System.out.println();
            System.out.println("ようこそ、" + account.getAccountHolder() + " 様");
            return account;
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
            return null;
        }
    }

    private void runSession(Account account) {
        while (true) {
            printMenu();
            String choice = scanner.nextLine().trim();
            System.out.println();
            switch (choice) {
                case "1" -> checkBalance(account);
                case "2" -> deposit(account);
                case "3" -> withdraw(account);
                case "4" -> showHistory(account);
                case "5" -> {
                    System.out.println("ありがとうございました。カードをお取りください。");
                    System.out.println("─────────────────────────────────────");
                    return;
                }
                default -> System.out.println("[ERROR] 無効な選択です。1〜5を入力してください。");
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("─────────────────────────────────────");
        System.out.println("  1. 残高照会  (Balance Inquiry)");
        System.out.println("  2. 入金      (Deposit)");
        System.out.println("  3. 出金      (Withdraw)");
        System.out.println("  4. 取引履歴  (Transaction History)");
        System.out.println("  5. 終了      (Exit / Logout)");
        System.out.println("─────────────────────────────────────");
        System.out.print("選択してください: ");
    }

    private void checkBalance(Account account) {
        long balance = accountService.getBalance(account.getId());
        System.out.printf("現在の残高: ¥%,d%n", balance);
    }

    private void deposit(Account account) {
        System.out.print("入金額を入力してください (¥): ");
        try {
            long amount = Long.parseLong(scanner.nextLine().trim().replace(",", ""));
            accountService.deposit(account.getId(), amount);
            long newBalance = accountService.getBalance(account.getId());
            System.out.printf("入金完了。新しい残高: ¥%,d%n", newBalance);
        } catch (NumberFormatException e) {
            System.out.println("[ERROR] 無効な金額です。数字を入力してください。");
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    private void withdraw(Account account) {
        System.out.print("出金額を入力してください (¥): ");
        try {
            long amount = Long.parseLong(scanner.nextLine().trim().replace(",", ""));
            accountService.withdraw(account.getId(), amount);
            long newBalance = accountService.getBalance(account.getId());
            System.out.printf("出金完了。新しい残高: ¥%,d%n", newBalance);
        } catch (NumberFormatException e) {
            System.out.println("[ERROR] 無効な金額です。数字を入力してください。");
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    private void showHistory(Account account) {
        List<Transaction> transactions = accountService.getTransactionHistory(account.getId());
        if (transactions.isEmpty()) {
            System.out.println("取引履歴がありません。");
            return;
        }
        System.out.println("─────────────────────────────────────────────────────────");
        System.out.printf("%-18s %-10s %12s %16s%n", "日時", "種別", "金額", "残高");
        System.out.println("─────────────────────────────────────────────────────────");
        for (Transaction tx : transactions) {
            System.out.printf("%-18s %-10s %,12d %,16d%n",
                    tx.getTransactionTime().format(FORMATTER),
                    tx.getType(),
                    tx.getAmount(),
                    tx.getBalanceAfter());
        }
        System.out.println("─────────────────────────────────────────────────────────");
    }
}
