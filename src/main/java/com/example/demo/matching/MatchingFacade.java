package com.example.demo.matching;

import java.util.List;
import java.util.Map;

/**
 * The only entry point other modules may use to reach matches.
 *
 * A single method, because after the match endpoints moved onto MatchController only one thing is
 * still asked from outside: the badge count on a listing.
 */
public interface MatchingFacade {

    /** Match counts per listing id, for the badge on the owner's own listings. */
    Map<Long, Long> countMatchesFor(List<Long> reportIds);
}
