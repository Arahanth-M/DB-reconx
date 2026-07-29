package com.dbtraining.reconx.service;

import com.dbtraining.reconx.dto.ReconResult;
import com.dbtraining.reconx.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReconciliationEngineTest {

    private final ReconciliationEngine engine = new ReconciliationEngine();

    @Test
    void testReconcile_exactMatch_returnsMatched() {
        // Given
        EquityTrade internal = equity("EQU-20260603-0001", "100.00", "1000");
        EquityTrade external = equity("EQU-20260603-0001", "100.00", "1000");

        // When
        List<ReconResult> out = engine.reconcile(
                List.of(internal),
                List.of(external),
                ReconciliationRule.EXACT
        );

        // Then
        assertThat(out).hasSize(1);
        assertThat(out.get(0).status()).isEqualTo(ReconResult.Status.MATCHED);
    }

    @ParameterizedTest(name = "price diff {0} stays within 1% tolerance")
    @ValueSource(strings = {"0.10", "0.50", "0.99"})
    void testReconcile_priceTolerance_withinThreshold(String diff) {

        BigDecimal basePrice = new BigDecimal("100.00");

        EquityTrade internal = equity("EQU-20260603-0002", "100.00", "1000");
        EquityTrade external = equity(
                "EQU-20260603-0002",
                basePrice.add(new BigDecimal(diff)).toPlainString(),
                "1000"
        );

        List<ReconResult> out = engine.reconcile(
                List.of(internal),
                List.of(external),
                ReconciliationRule.PRICE_TOLERANCE_1PCT
        );

        assertThat(out).hasSize(1);
        assertThat(out.get(0).status()).isEqualTo(ReconResult.Status.MATCHED);
    }

    @Test
    void testReconcile_missingCounterpartyTrade_returnsBreak() {

        EquityTrade internal = equity("EQU-20260603-0003", "100.00", "1000");

        List<ReconResult> out = engine.reconcile(
                List.of(internal),
                List.of(),
                ReconciliationRule.EXACT
        );

        assertThat(out).hasSize(1);
        assertThat(out.get(0).status()).isEqualTo(ReconResult.Status.BREAK);
        assertThat(out.get(0).discrepancyType()).isEqualTo("MISSING_EXTERNAL");
    }

    @Test
    void testReconcile_emptyInternal_returnsEmpty() {

        List<ReconResult> out = engine.reconcile(
                List.of(),
                List.of(),
                ReconciliationRule.EXACT
        );

        assertThat(out).isEmpty();
    }

    @Test
    void testReconcile_allMismatched_summaryShowsZeroMatched() {

        List<TradeType> internals = List.of(
                equity("EQU-MM-1", "100.00", "1000"),
                equity("EQU-MM-2", "100.00", "1000"),
                equity("EQU-MM-3", "100.00", "1000")
        );

        List<TradeType> externals = List.of(
                equity("EQU-MM-1", "200.00", "1000"),
                equity("EQU-MM-2", "200.00", "1000"),
                equity("EQU-MM-3", "200.00", "1000")
        );

        List<ReconResult> out = engine.reconcile(
                internals,
                externals,
                ReconciliationRule.EXACT
        );

        ReconSummary summary = out.stream()
                .collect(new ReconSummaryCollector());

        assertThat(summary.total()).isEqualTo(3);
        assertThat(summary.matched()).isEqualTo(0);
        assertThat(summary.broken()).isEqualTo(3);
    }

    private EquityTrade equity(String ref, String price, String qty) {
        return EquityTrade.builder()
                .tradeRef(TradeRef.of(ref))
                .instrumentSymbol("SAP.DE")
                .price(new BigDecimal(price))
                .quantity(new BigDecimal(qty))
                .currency("EUR")
                .side(Side.BUY)
                .tradeDate(LocalDate.of(2026, 6, 3))
                .counterpartyId(1L)
                .build();
    }
}