package edu.sysu.snowfield.support;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import edu.sysu.snowfield.booking.application.BookingApplicationService;
import edu.sysu.snowfield.booking.application.CreateLodgingOrderCommand;
import edu.sysu.snowfield.booking.application.CreatedLodgingOrder;
import edu.sysu.snowfield.identity.application.AccessTokenPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 真实服务器 + 真实 PostgreSQL 的 JWT、角色、对象所有权与支付 HTTP 集成测试。 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SecurityPaymentWebIT {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @LocalServerPort
    private int port;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private BookingApplicationService bookingService;

    @Autowired
    private AccessTokenPort accessTokenPort;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private long userId;
    private long otherUserId;
    private long resourceUnitId;
    private CreatedLodgingOrder order;

    @BeforeEach
    void setUp() throws SQLException {
        execute("""
                TRUNCATE TABLE user_role, app_user, campground, season, resource_type, holiday, price_rule,
                               package_offer, lodging_reservation
                RESTART IDENTITY CASCADE
                """);
        execute("""
                INSERT INTO app_user (email, password_hash, display_name) VALUES
                    ('http-owner@example.com', 'x', 'HTTP付款人'),
                    ('http-other@example.com', 'x', '其他用户')
                """);
        userId = scalar("SELECT user_id FROM app_user WHERE email = 'http-owner@example.com'");
        otherUserId = scalar("SELECT user_id FROM app_user WHERE email = 'http-other@example.com'");
        execute("INSERT INTO campground (code, name, timezone, currency) VALUES ('main', '雪野', 'Asia/Shanghai', 'CNY')");
        long campgroundId = scalar("SELECT campground_id FROM campground WHERE code = 'main'");
        execute("""
                INSERT INTO resource_type (campground_id, code, name, kind, base_capacity, base_price)
                VALUES (%d, 'CABIN', '木屋', 'CABIN', 4, 1280.00)
                """.formatted(campgroundId));
        long typeId = scalar("SELECT resource_type_id FROM resource_type WHERE code = 'CABIN'");
        execute("INSERT INTO resource_type_stay_mode (resource_type_id, stay_mode) VALUES (%d, 'OVERNIGHT')"
                .formatted(typeId));
        execute("INSERT INTO resource_unit (resource_type_id, code, name, capacity) VALUES (%d, 'HTTP-A', 'HTTP测试屋', 4)"
                .formatted(typeId));
        resourceUnitId = scalar("SELECT resource_unit_id FROM resource_unit WHERE code = 'HTTP-A'");
        order = createOrder("http-order");
    }

    @Test
    void missingBearerTokenReturnsContractAuthRequired() {
        assertError(() -> paymentRequest(order.orderId(), null, "pay-missing", "SUCCESS"),
                HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED");
    }

    @Test
    void malformedBearerTokenReturnsContractTokenInvalid() {
        assertError(() -> paymentRequest(order.orderId(), "not-a-jwt", "pay-invalid", "SUCCESS"),
                HttpStatus.UNAUTHORIZED, "TOKEN_INVALID");
    }

    @Test
    void expiredBearerTokenReturnsContractTokenExpired() {
        assertError(() -> paymentRequest(order.orderId(), expiredToken(), "pay-expired", "SUCCESS"),
                HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED");
    }

    @Test
    void signedTokenWithInvalidSubjectOrRoleIsRejected() {
        assertError(() -> paymentRequest(order.orderId(), signedToken("not-a-user", List.of("CUSTOMER"), 300),
                        "pay-bad-subject", "SUCCESS"),
                HttpStatus.UNAUTHORIZED, "TOKEN_INVALID");
        assertError(() -> paymentRequest(order.orderId(), signedToken(Long.toString(userId), List.of("ROOT"), 300),
                        "pay-bad-role", "SUCCESS"),
                HttpStatus.UNAUTHORIZED, "TOKEN_INVALID");
    }

    @Test
    void issuedAccessTokenHasThirtyMinuteLifetimeAndExpectedClaims() {
        String token = token(userId, "CUSTOMER");
        var jwt = jwtDecoder.decode(token);

        assertThat(jwt.getIssuer().toString()).isEqualTo("https://snowfield.test");
        assertThat(jwt.getSubject()).isEqualTo(Long.toString(userId));
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("CUSTOMER");
        assertThat(jwt.getExpiresAt().getEpochSecond() - jwt.getIssuedAt().getEpochSecond()).isEqualTo(1800);
    }

    @Test
    void passwordEncoderUsesAdaptiveOneWayHash() {
        String encoded = passwordEncoder.encode("correct horse battery staple");

        assertThat(encoded).startsWith("$2").doesNotContain("correct horse battery staple");
        assertThat(passwordEncoder.matches("correct horse battery staple", encoded)).isTrue();
        assertThat(passwordEncoder.matches("wrong", encoded)).isFalse();
    }

    @Test
    void customerCannotEnterAdminNamespaceButAdminReachesRouting() {
        String customer = token(userId, "CUSTOMER");
        assertError(() -> get("/api/v1/admin/not-created", customer),
                HttpStatus.FORBIDDEN, "ACCESS_DENIED");

        String admin = token(userId, "ADMIN");
        assertError(() -> get("/api/v1/admin/not-created", admin),
                HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

    @Test
    void authenticatedOwnerPaysAndResponseUsesContractStringIds() throws Exception {
        ResponseEntity<String> response = paymentRequest(
                order.orderId(), token(userId, "CUSTOMER"), "pay-http-success", "SUCCESS");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = JSON.readTree(response.getBody());
        assertThat(body.get("success").asBoolean()).isTrue();
        assertThat(body.get("data").get("id").asText()).isNotBlank();
        assertThat(body.get("data").get("orderId").asText()).isEqualTo(Long.toString(order.orderId()));
        assertThat(body.get("data").get("status").asText()).isEqualTo("SUCCEEDED");
        assertThat(body.get("data").get("amount").asText()).isEqualTo("1280.00");
        assertThat(body.get("data").get("method").asText()).isEqualTo("SIMULATED");
        assertThat(body.get("data").has("replayed")).isFalse();
        assertThat(scalar("SELECT count(*) FROM payment WHERE order_id = " + order.orderId()
                + " AND status = 'SUCCEEDED'")).isEqualTo(1);
    }

    @Test
    void sameHttpIdempotencyKeyReplaysWithoutSecondPayment() throws Exception {
        String owner = token(userId, "CUSTOMER");
        JsonNode first = JSON.readTree(paymentRequest(
                order.orderId(), owner, "pay-http-replay", "SUCCESS").getBody());
        JsonNode second = JSON.readTree(paymentRequest(
                order.orderId(), owner, "pay-http-replay", "SUCCESS").getBody());

        assertThat(second.get("data").get("id").asText())
                .isEqualTo(first.get("data").get("id").asText());
        assertThat(scalar("SELECT count(*) FROM payment WHERE order_id = " + order.orderId())).isEqualTo(1);
    }

    @Test
    void authenticatedNonOwnerGetsOrderNotFoundAndNoPayment() throws SQLException {
        assertError(() -> paymentRequest(order.orderId(), token(otherUserId, "CUSTOMER"),
                        "pay-http-other", "SUCCESS"),
                HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND");
        assertThat(scalar("SELECT count(*) FROM payment WHERE order_id = " + order.orderId())).isZero();
    }

    @Test
    void missingIdempotencyHeaderReturnsValidationError() {
        assertError(() -> paymentRequest(order.orderId(), token(userId, "CUSTOMER"), null, "SUCCESS"),
                HttpStatus.BAD_REQUEST, "VALIDATION_FAILED");
    }

    @Test
    void invalidOutcomeReturnsValidationErrorWithoutWritingPayment() throws SQLException {
        assertError(() -> paymentRequest(order.orderId(), token(userId, "CUSTOMER"),
                        "pay-http-bad", "MAYBE"),
                HttpStatus.BAD_REQUEST, "VALIDATION_FAILED");
        assertThat(scalar("SELECT count(*) FROM payment WHERE order_id = " + order.orderId())).isZero();
    }

    @Test
    void corsAllowsConfiguredFrontendAndRejectsUnknownOrigin() {
        ResponseEntity<String> allowed = client().options()
                .uri("/api/v1/orders/{id}/payments/simulate", order.orderId())
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .retrieve().toEntity(String.class);
        assertThat(allowed.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:5173");

        assertThatThrownBy(() -> client().options()
                .uri("/api/v1/orders/{id}/payments/simulate", order.orderId())
                .header("Origin", "https://evil.example")
                .header("Access-Control-Request-Method", "POST")
                .retrieve().toEntity(String.class))
                .isInstanceOf(HttpClientErrorException.class)
                .satisfies(thrown -> {
                    HttpClientErrorException ex = (HttpClientErrorException) thrown;
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(ex.getResponseHeaders().getAccessControlAllowOrigin()).isNull();
                    assertThat(ex.getResponseBodyAsString()).doesNotContain("https://evil.example");
                    try {
                        JsonNode body = JSON.readTree(ex.getResponseBodyAsString());
                        assertThat(body.get("success").asBoolean()).isFalse();
                        assertThat(body.get("error").get("code").asText()).isEqualTo("ACCESS_DENIED");
                    } catch (Exception parseFailure) {
                        throw new AssertionError("CORS拒绝响应不是统一JSON", parseFailure);
                    }
                });
    }

    private ResponseEntity<String> paymentRequest(long orderId, String token, String idempotencyKey,
                                                   String outcome) {
        RestClient.RequestBodySpec request = client().post()
                .uri("/api/v1/orders/{id}/payments/simulate", orderId)
                .contentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if (idempotencyKey != null) {
            request.header("Idempotency-Key", idempotencyKey);
        }
        return request.body("{\"desiredOutcome\":\"" + outcome + "\"}")
                .retrieve().toEntity(String.class);
    }

    private ResponseEntity<String> get(String path, String token) {
        return client().get().uri(path)
                .header("Authorization", "Bearer " + token)
                .retrieve().toEntity(String.class);
    }

    private RestClient client() {
        return RestClient.create("http://127.0.0.1:" + port);
    }

    private String token(long subject, String role) {
        return accessTokenPort.issue(subject, Set.of(role)).accessToken();
    }

    private String expiredToken() {
        return signedToken(Long.toString(userId), List.of("CUSTOMER"), -120);
    }

    private String signedToken(String subject, List<String> roles, long expiresInSeconds) {
        Instant now = Instant.now();
        Instant issuedAt = expiresInSeconds < 0 ? now.minusSeconds(600) : now;
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("https://snowfield.test")
                .subject(subject)
                .issuedAt(issuedAt)
                .expiresAt(now.plusSeconds(expiresInSeconds))
                .claim("roles", roles)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private void assertError(ThrowingCall call, HttpStatus status, String code) {
        assertThatThrownBy(call::run)
                .isInstanceOf(HttpClientErrorException.class)
                .satisfies(thrown -> {
                    HttpClientErrorException ex = (HttpClientErrorException) thrown;
                    assertThat(ex.getStatusCode()).isEqualTo(status);
                    try {
                        JsonNode body = JSON.readTree(ex.getResponseBodyAsString());
                        assertThat(body.get("success").asBoolean()).isFalse();
                        assertThat(body.get("error").get("code").asText()).isEqualTo(code);
                        assertThat(body.get("traceId").asText()).isNotBlank();
                        assertThat(ex.getResponseBodyAsString())
                                .doesNotContain("Exception", "java.", "org.springframework", "SQL");
                    } catch (Exception parseFailure) {
                        throw new AssertionError("错误响应不是合法契约 JSON", parseFailure);
                    }
                });
    }

    private CreatedLodgingOrder createOrder(String key) {
        var line = new CreateLodgingOrderCommand.LodgingLine(
                resourceUnitId, "OVERNIGHT",
                OffsetDateTime.parse("2027-12-20T15:00+08"),
                OffsetDateTime.parse("2027-12-21T12:00+08"), 2);
        return bookingService.createLodgingOrder(
                new CreateLodgingOrderCommand(userId, key, List.of(line), null));
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private long scalar(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    @FunctionalInterface
    private interface ThrowingCall {
        void run() throws Exception;
    }
}
