package com.sporty.f1bets.betting.application;

import com.sporty.f1bets.betting.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}

