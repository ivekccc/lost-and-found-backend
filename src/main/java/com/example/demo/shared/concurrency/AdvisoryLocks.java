package com.example.demo.shared.concurrency;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AdvisoryLocks {

    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.MANDATORY)
    public void lock(LockScope scope, Object key) {
        entityManager
                .createNativeQuery("SELECT 1 FROM (SELECT pg_advisory_xact_lock(:scope, hashtext(:key))) AS acquired")
                .setParameter("scope", scope.getId())
                .setParameter("key", String.valueOf(key))
                .getSingleResult();
    }
}
