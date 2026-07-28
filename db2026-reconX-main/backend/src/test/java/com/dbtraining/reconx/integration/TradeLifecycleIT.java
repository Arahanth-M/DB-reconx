package com.dbtraining.reconx.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * ============================================================================
 * TICKET-ADV078 — Full lifecycle integration test with Testcontainers
 *
 * WHAT:    Spins up a real postgres:16-alpine container, boots the entire
 *          Spring context, and drives a realistic trade lifecycle over HTTP:
 *          login → create → list → patch-status → recon-run → resolve.
 * WHY:     Proves the full stack end-to-end against a real database engine,
 *          catching bugs that the H2 dev profile may mask.
 * OBSERVE: A postgres:16-alpine container starts, all 6 ordered tests pass,
 *          and the container is reaped at the end.
 * ============================================================================
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TradeLifecycleIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @LocalServerPort int port;
    @Autowired ObjectMapper om;

    static String token;
    static Long createdId;
    static String reconJobId;
    static Long breakId;

    RestTemplate http = new RestTemplate();

    private HttpHeaders authHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return h;
    }

    @Test @Order(1)
    void loginAsAdmin() {
        var body = """
                {"username":"admin@db.com","password":"admin123"}
                """;
        var req = new HttpEntity<>(body, new HttpHeaders() {{
            setContentType(MediaType.APPLICATION_JSON);
        }});
        var resp = http.postForEntity(
                "http://localhost:" + port + "/api/auth/login", req, JsonNode.class);
        Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());
        token = resp.getBody().get("token").asText();
        Assertions.assertNotNull(token);
    }

    @Test @Order(2)
    void createTrade() {
        var body = """
                {"tradeRef":"INT-20260315-0001","instrumentId":1,"counterpartyId":1,
                 "assetClass":"EQUITY","side":"BUY",
                 "quantity":100.0,"price":245.50,"tradeDate":"2026-03-15"}
                """;
        var resp = http.exchange(
                "http://localhost:" + port + "/api/v1/trades",
                HttpMethod.POST, new HttpEntity<>(body, authHeaders()), JsonNode.class);
        Assertions.assertEquals(HttpStatus.CREATED, resp.getStatusCode());
        createdId = resp.getBody().get("id").asLong();
    }

    @Test @Order(3)
    void getTradeBack() {
        var resp = http.exchange(
                "http://localhost:" + port + "/api/v1/trades?status=PENDING",
                HttpMethod.GET, new HttpEntity<>(authHeaders()), JsonNode.class);
        Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());
        Assertions.assertTrue(resp.getBody().get("totalElements").asLong() >= 1);
    }

    @Test @Order(4)
    void patchStatus() {
        var body = """
                {"status":"MATCHED"}
                """;
        var resp = http.exchange(
                "http://localhost:" + port + "/api/v1/trades/" + createdId + "/status",
                HttpMethod.PATCH, new HttpEntity<>(body, authHeaders()), JsonNode.class);
        Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());
        Assertions.assertEquals("MATCHED", resp.getBody().get("status").asText());
    }

    @Test @Order(5)
    void triggerRecon() {
        var body = """
                {"from":"2026-03-01","to":"2026-03-31"}
                """;
        var resp = http.exchange(
                "http://localhost:" + port + "/api/v1/recon/run",
                HttpMethod.POST, new HttpEntity<>(body, authHeaders()), JsonNode.class);
        Assertions.assertEquals(HttpStatus.ACCEPTED, resp.getStatusCode());
        reconJobId = resp.getBody().get("jobId").asText();
    }

    @Test @Order(6)
    void resolveBreak() {
        // Test data seeded by Liquibase guarantees at least one break with id 1.
        breakId = 1L;
        var body = """
                {"note":"Confirmed via counterparty email on 2026-03-16."}
                """;
        var resp = http.exchange(
                "http://localhost:" + port + "/api/v1/recon/results/" + breakId + "/resolve",
                HttpMethod.PUT, new HttpEntity<>(body, authHeaders()), JsonNode.class);
        Assertions.assertEquals(HttpStatus.OK, resp.getStatusCode());
        Assertions.assertEquals("RESOLVED", resp.getBody().get("status").asText());
    }
}
