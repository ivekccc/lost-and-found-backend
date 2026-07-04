package com.example.demo.repository;

import com.example.demo.model.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    Optional<Challenge> findByReportIdAndAuthorId(Long reportId, Long authorId);

    List<Challenge> findByReportIdOrderByCreatedAtAsc(Long reportId);

    List<Challenge> findByAuthorIdOrderByCreatedAtDesc(Long authorId);
}
