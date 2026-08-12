package com.example.demo.report;

import com.example.demo.report.model.Report;
import com.example.demo.report.model.ReportCategory;
import com.example.demo.report.dto.MatchCandidateView;

import java.util.List;
import java.util.Optional;

/**
 * The only entry point other modules may use to reach listings and their categories.
 *
 * Write operations are phrased as domain actions rather than repository calls, so the rules that
 * govern them stay in this module. Blocking a user, for instance, used to be a loop over
 * {@code findByUserId} inside the admin module, which meant that module had to know what a hidden
 * listing looks like.
 */
public interface ReportFacade {

    Report requireById(Long reportId);

    Optional<Report> findById(Long reportId);

    /** Same as {@link #requireById} but treats a soft-deleted listing as missing. */
    Report requireVisibleById(Long reportId);

    /** Loads with a row lock, for callers that decide on claims and must serialise with each other. */
    Report lockById(Long reportId);

    Report getReference(Long reportId);

    List<MatchCandidateView> findMatchCandidates(String type, Long categoryId, Long ownerId,
                                                 String probeText, Long cityId);

    /** Active lost listings, for the periodic matching rescan. */
    List<Report> findActiveLostListings();

    ReportCategory requireCategoryById(Long categoryId);

    boolean categoryExists(Long categoryId);

    void saveCategory(ReportCategory category);

    /** Hides the listings of a user who was just blocked; returns their ids so the caller can
     *  settle whatever else hangs off them. */
    List<Long> hideListingsOfBlockedUser(Long userId);

    void restoreListingsHiddenByBlock(Long userId);

    /** Deletes a user's listings and returns the Cloudinary ids that must be cleaned up. */
    List<String> deleteListingsOfUser(Long userId);

    void resolveAbuseReportsForUser(Long userId, String adminEmail);

    void dismissAbuseReportsForUser(Long userId, String adminEmail);
}
