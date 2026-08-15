package com.example.demo.matching.repository;

import com.example.demo.matching.model.MatchingConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchingConfigRepository extends JpaRepository<MatchingConfig, Long> {

    /**
     * The row is created by migration V54 and cannot be deleted through the API, so its absence is
     * a broken schema rather than a missing resource.
     */
    default MatchingConfig require() {
        return findById(MatchingConfig.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("matching_config row is missing"));
    }
}
