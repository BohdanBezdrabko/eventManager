package com.example.sportadministrationsystem.service;

import com.example.sportadministrationsystem.repository.DbLockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DbLockService {

    private final DbLockRepository repo;

    public boolean tryLock(long key) {
        return repo.tryLock(key);
    }

    public void unlock(long key) {
        repo.unlock(key);
    }

    /** зручний ключ на основі рядка */
    public static long key(String name) {
        // простий хеш (не криптографічний), стабільний для advisory lock
        return name.hashCode();
    }
}
