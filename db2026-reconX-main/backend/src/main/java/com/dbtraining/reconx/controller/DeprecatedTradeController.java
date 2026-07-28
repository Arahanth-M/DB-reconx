package com.dbtraining.reconx.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ============================================================================
 * TICKET-ADV080 — Deprecated v0 trade surface area
 *
 * WHAT:    A stub that responds to the retired /v0/trades path with HTTP 410
 *          Gone and standard deprecation headers.
 * WHY:     Returning 410 (not 404) tells API consumers the resource existed but
 *          was deliberately retired. The Sunset and Link headers guide them to
 *          the successor.
 * OBSERVE: curl -i /api/v0/trades → 410 with Deprecation/Sunset/Link headers.
 * ============================================================================
 */
@RestController
@RequestMapping("/v0/trades")
@Hidden  // exclude from Swagger UI
public class DeprecatedTradeController {

    @Deprecated(since = "v1.4.0", forRemoval = true)
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deprecatedList(HttpServletResponse response) {
        response.setHeader("Deprecation", "true");
        response.setHeader("Sunset", "Sat, 1 Jul 2026 00:00:00 GMT");
        response.setHeader("Link",
                "</api/v1/trades?status=...>; rel=\"successor-version\"");
        return ResponseEntity.status(HttpStatus.GONE).build();
    }
}
