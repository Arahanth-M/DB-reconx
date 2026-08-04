package com.dbtraining.reconx.repository;

import com.dbtraining.reconx.repository.entity.Trade;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

/**
 * ============================================================================
 * TICKET-ADV056 — TradeSpecifications
 *
 * WHAT:    Static factories that return Specification<Trade> instances which
 *          callers compose with .and() / .or() in the service layer for
 *          dynamic queries.
 * HOW:     Each method returns a lambda `(root, query, cb) -> Predicate`.
 *          A null filter argument means "no constraint", which is encoded
 *          via cb.conjunction().
 * WHY:     Avoids exploding the repository with `findByXAndYAndZ...`
 *          methods for every possible combination of filters.
 * OBSERVE: GET /api/v1/trades?status=NEW&from=2026-01-01 should produce the
 *          right SQL WHERE clause — turn on `spring.jpa.show-sql` to verify.
 * ============================================================================
 */
public final class TradeSpecifications {

    private TradeSpecifications() {}

    /** Eager-fetch associations for mapping after the TX closes (open-in-view=false). */
    public static Specification<Trade> withAssociations() {
        return (root, query, cb) -> {
            // Skip fetch on the count query Spring Data runs for Page.
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("instrument", JoinType.LEFT);
                root.fetch("counterparty", JoinType.LEFT);
                query.distinct(true);
            }
            return cb.conjunction();
        };
    }

    public static Specification<Trade> hasStatus(String status) {
        return (root, q, cb) -> status == null
                ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }

    public static Specification<Trade> tradeDateBetween(LocalDate from, LocalDate to) {
        return (root, q, cb) -> {
            if (from == null && to == null) return cb.conjunction();
            if (from == null) return cb.lessThanOrEqualTo(root.get("tradeDate"), to);
            if (to == null)   return cb.greaterThanOrEqualTo(root.get("tradeDate"), from);
            return cb.between(root.get("tradeDate"), from, to);
        };
    }

    public static Specification<Trade> hasCounterparty(Long counterpartyId) {
        return (root, q, cb) -> counterpartyId == null
                ? cb.conjunction()
                : cb.equal(root.get("counterparty").get("id"), counterpartyId);
    }
}
