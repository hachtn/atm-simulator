package com.atm.atm_simulator.simulator;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.atm.atm_simulator.model.Account;
import com.atm.atm_simulator.model.Transaction;
import com.atm.atm_simulator.service.AccountService;
import com.atm.atm_simulator.service.AtmService;

@Component
@Order(2)
@ConditionalOnProperty(name = "atm.simulator.cli.enabled", havingValue = "true")
public class AtmSimulator implements CommandLineRunner {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final AtmService atmService;
    private final AccountService accountService;

    public AtmSimulator(AtmService atmService, AccountService accountService) {
        this.atmService = atmService;
        this.accountService = accountService;
    }

    @Override
    public void run(String... args) {
        printBanner();
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                Account account = authenticate(scanner);
                if (account != null) {
                    boolean exitApp = runSession(account, scanner);
                    if (exitApp) {
                        break;
                    }
                }
            }
        }
        System.out.println("システムを終了します。ありがとうございました。");
    }

    private void printBanner() {
        System.out.println();
        System.out.println("╔══════════════════════════════════╗");
        System.out.println("║     ATM SIMULATOR - MUFG Bank   ║");
        System.out.println("╚══════════════════════════════════╝");
    }

    private Account authenticate(Scanner scanner) {
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

    private boolean runSession(Account account, Scanner scanner) {
        while (true) {
            printMenu();
            String choice = scanner.nextLine().trim();
            System.out.println();
            switch (choice) {
                case "1" -> checkBalance(account);
                case "2" -> deposit(account, scanner);
                case "3" -> withdraw(account, scanner);
                case "4" -> showHistory(account);
                case "5" -> {
                    System.out.println("ありがとうございました。カードをお取りください。");
                    System.out.println("─────────────────────────────────────");
                    return false;
                }
                case "0" -> {
                    return true;
                }
                default -> System.out.println("[ERROR] 無効な選択です。0〜5を入力してください。");
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
        System.out.println("  5. ログアウト (Logout)");
        System.out.println("  0. 終了      (Exit Application)");
        System.out.println("─────────────────────────────────────");
        System.out.print("選択してください: ");
    }

    private void checkBalance(Account account) {
        long balance = accountService.getBalance(account.getAccountNumber());
        System.out.printf("現在の残高: ¥%,d%n", balance);
    }

    private void deposit(Account account, Scanner scanner) {
        System.out.print("入金額を入力してください (¥): ");
        try {
            long amount = Long.parseLong(scanner.nextLine().trim().replace(",", ""));
            long newBalance = accountService.deposit(account.getAccountNumber(), amount);
            System.out.printf("入金完了。新しい残高: ¥%,d%n", newBalance);
        } catch (NumberFormatException e) {
            System.out.println("[ERROR] 無効な金額です。数字を入力してください。");
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    private void withdraw(Account account, Scanner scanner) {
        System.out.print("出金額を入力してください (¥): ");
        try {
            long amount = Long.parseLong(scanner.nextLine().trim().replace(",", ""));
            long newBalance = accountService.withdraw(account.getAccountNumber(), amount);
            System.out.printf("出金完了。新しい残高: ¥%,d%n", newBalance);
        } catch (NumberFormatException e) {
            System.out.println("[ERROR] 無効な金額です。数字を入力してください。");
        } catch (Exception e) {
            System.out.println("[ERROR] " + e.getMessage());
        }
    }

    private void showHistory(Account account) {
        List<Transaction> transactions = accountService.getTransactionHistory(account.getAccountNumber());
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
