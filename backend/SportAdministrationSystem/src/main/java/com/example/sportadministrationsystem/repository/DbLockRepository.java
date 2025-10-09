package com.example.sportadministrationsystem.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DbLockRepository {

    private final JdbcTemplate jdbcTemplate;

    public boolean tryLock(long key) {
        Boolean res = jdbcTemplate.queryForObject(
                "select pg_try_advisory_lock(?)",
                Boolean.class,
                key
        );
        return Boolean.TRUE.equals(res);
    }

    public boolean unlock(long key) {
        Boolean res = jdbcTemplate.queryForObject(
                "select pg_advisory_unlock(?)",
                Boolean.class,
                key
        );
        return Boolean.TRUE.equals(res);
    }
}
