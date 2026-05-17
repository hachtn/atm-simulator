package com.atm.atm_simulator.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.atm.atm_simulator.model.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAccountIdOrderByTransactionTimeDesc(Long accountId);
}
