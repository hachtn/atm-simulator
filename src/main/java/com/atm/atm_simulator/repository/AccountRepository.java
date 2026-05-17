package com.atm.atm_simulator.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.atm.atm_simulator.model.Account;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);
}