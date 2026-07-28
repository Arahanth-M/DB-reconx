package com.dbtraining.reconx.controller;

import com.dbtraining.reconx.dto.PagedResponse;
import com.dbtraining.reconx.dto.TradeMapper;
import com.dbtraining.reconx.dto.TradeRequest;
import com.dbtraining.reconx.dto.TradeResponse;
import com.dbtraining.reconx.repository.entity.Trade;
import com.dbtraining.reconx.service.TradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.Map;

/**
 * ============================================================================
 * TICKET-ADV063 – TICKET-ADV067 — TradeController (full CRUD + filterable list)
 * TICKET-ADV080 — API versioning: every endpoint under /v1/
 *
 * Combined with the /api context-path from application.yml, full URLs are
 * /api/v1/trades, /api/v1/trades/{id} etc.
 * ============================================================================
 */
@RestController
@RequestMapping("/v1/trades")
@Tag(name = "trades", description = "Trade CRUD and search")
@SecurityRequirement(name = "bearerAuth")
public class TradeController {

    private final TradeService service;
    private final TradeMapper mapper;

    public TradeController(TradeService service, TradeMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    /**
     * TICKET-ADV063 — GET /api/v1/trades (paginated, filterable, sortable)
     */
    @GetMapping
    @Operation(summary = "List trades — paginated, filterable, sortable")
    public PagedResponse<TradeResponse> list(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long counterpartyId,
            @PageableDefault(size = 20, sort = "tradeDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<Trade> page = service.list(from, to, status, counterpartyId, pageable);
        return PagedResponse.from(page, mapper::toResponse);
    }

    /**
     * TICKET-ADV064 — POST /api/v1/trades (create + validation)
     */
    @PostMapping
    @Operation(summary = "Create a trade")
    public ResponseEntity<TradeResponse> create(@Valid @RequestBody TradeRequest req,
                                                @AuthenticationPrincipal Object principal) {
        String actor = String.valueOf(principal);
        Trade saved = service.create(req, actor);
        URI location = URI.create("/api/v1/trades/" + saved.getId());
        return ResponseEntity.created(location).body(mapper.toResponse(saved));
    }

    /**
     * TICKET-ADV065 — GET /api/v1/trades/{id} (get single trade by ID)
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get single trade by ID")
    public TradeResponse getById(@PathVariable Long id) {
        Trade trade = service.findById(id);
        return mapper.toResponse(trade);
    }

    /**
     * TICKET-ADV066 — PUT /api/v1/trades/{id} (full update of a trade)
     */
    @PutMapping("/{id}")
    @Operation(summary = "Full update of a trade")
    public TradeResponse update(@PathVariable Long id, @Valid @RequestBody TradeRequest req,
                                @AuthenticationPrincipal Object principal) {
        String actor = String.valueOf(principal);
        Trade updated = service.update(id, req, actor);
        return mapper.toResponse(updated);
    }

    /**
     * TICKET-ADV067 — PATCH /api/v1/trades/{id}/status (update status field)
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Update only the status field")
    public TradeResponse updateStatus(@PathVariable Long id,
                                      @RequestBody Map<String, String> body,
                                      @AuthenticationPrincipal Object principal) {
        String actor = String.valueOf(principal);
        String status = body != null ? body.get("status") : null;
        Trade saved = service.updateStatus(id, status, actor);
        return mapper.toResponse(saved);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete (sets deleted_at)")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal Object principal) {
        String actor = String.valueOf(principal);
        service.softDelete(id, actor);
        return ResponseEntity.noContent().build();
    }

    /**
     * TICKET-ADV080 — Deprecated v1 trade search endpoint example.
     * Returns 410 Gone with Deprecation / Sunset / Link headers.
     */
    @Deprecated(since = "v1.4.0", forRemoval = true)
    @GetMapping(value = "/old-search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> oldSearch(HttpServletResponse response) {
        response.setHeader("Deprecation", "true");
        response.setHeader("Sunset", "Sat, 1 Jul 2026 00:00:00 GMT");
        response.setHeader("Link",
                "</api/v1/trades?status=...>; rel=\"successor-version\"");
        return ResponseEntity.status(HttpStatus.GONE).build();
    }
}
