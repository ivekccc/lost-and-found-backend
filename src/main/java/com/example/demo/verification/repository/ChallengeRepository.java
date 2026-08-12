package com.example.demo.verification.repository;

import com.example.demo.verification.model.Challenge;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {

    Optional<Challenge> findByReportIdAndAuthorId(Long reportId, Long authorId);

    List<Challenge> findByReportIdOrderByCreatedAtAsc(Long reportId);

    List<Challenge> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    /**
     * Challenges sitting on a given user's listings. Lets account deletion clear verification
     * history without the user module having to walk the listings itself.
     */
    @Query("SELECT c FROM Challenge c WHERE c.report.user.id = :ownerId ORDER BY c.createdAt ASC")
    List<Challenge> findByReportOwner(@Param("ownerId") Long ownerId);
}
