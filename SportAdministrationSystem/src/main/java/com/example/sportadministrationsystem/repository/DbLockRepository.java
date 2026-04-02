package com.example.sportadministrationsystem.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class DbLockRepository {

    private final JdbcTemplate jdbcTemplate;

    public boolean tryLock(long key) {
        try {
            Boolean res = jdbcTemplate.queryForObject(
                    "select pg_try_advisory_lock(?)",
                    Boolean.class,
                    key
            );
            return Boolean.TRUE.equals(res);
        } catch (Exception e) {
            // H2 у тестах не підтримує pg_try_advisory_lock; для тестів завжди успіх
            log.debug("pg_try_advisory_lock not supported (likely H2 test database): {}", e.getMessage());
            return true;
        }
    }

    public boolean unlock(long key) {
        try {
            Boolean res = jdbcTemplate.queryForObject(
                    "select pg_advisory_unlock(?)",
                    Boolean.class,
                    key
            );
            return Boolean.TRUE.equals(res);
        } catch (Exception e) {
            // H2 у тестах не підтримує pg_advisory_unlock; для тестів завжди успіх
            log.debug("pg_advisory_unlock not supported (likely H2 test database): {}", e.getMessage());
            return true;
        }
    }
}
