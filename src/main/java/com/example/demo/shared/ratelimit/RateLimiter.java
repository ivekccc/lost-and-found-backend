package com.example.demo.shared.ratelimit;

import com.example.demo.shared.exception.RateLimitExceededException;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.EstimationProbe;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class RateLimiter {

    private static final String MESSAGE_KEY = "rateLimit.tooManyRequests";
    private static final long NANOS_PER_SECOND = Duration.ofSeconds(1).toNanos();

    private final ConcurrentMap<String, TrackedBucket> buckets = new ConcurrentHashMap<>();

    public void consume(RateLimitPolicy policy, Object key) {
        ConsumptionProbe probe = bucket(policy, key).tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long seconds = Math.max(1, Math.ceilDiv(probe.getNanosToWaitForRefill(), NANOS_PER_SECOND));
            throw new RateLimitExceededException(MESSAGE_KEY, seconds);
        }
    }

    public void requireAvailable(RateLimitPolicy policy, Object key) {
        EstimationProbe probe = bucket(policy, key).estimateAbilityToConsume(1);
        if (!probe.canBeConsumed()) {
            long seconds = Math.max(1, Math.ceilDiv(probe.getNanosToWaitForRefill(), NANOS_PER_SECOND));
            throw new RateLimitExceededException(MESSAGE_KEY, seconds);
        }
    }

    public boolean tryConsume(RateLimitPolicy policy, Object key) {
        return bucket(policy, key).tryConsume(1);
    }

    @Scheduled(fixedDelayString = "PT10M")
    public void evictIdle() {
        long now = System.nanoTime();
        buckets.entrySet().removeIf(entry -> now - entry.getValue().lastUsed > entry.getValue().policy.period().toNanos());
    }

    private Bucket bucket(RateLimitPolicy policy, Object key) {
        TrackedBucket tracked = buckets.computeIfAbsent(policy.name() + ":" + key, ignored -> new TrackedBucket(policy, create(policy)));
        tracked.lastUsed = System.nanoTime();
        return tracked.bucket;
    }

    private static Bucket create(RateLimitPolicy policy) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(policy.capacity())
                        .refillGreedy(policy.capacity(), policy.period())
                        .build())
                .build();
    }

    private static final class TrackedBucket {
        private final RateLimitPolicy policy;
        private final Bucket bucket;
        private volatile long lastUsed;

        private TrackedBucket(RateLimitPolicy policy, Bucket bucket) {
            this.policy = policy;
            this.bucket = bucket;
        }
    }
}
