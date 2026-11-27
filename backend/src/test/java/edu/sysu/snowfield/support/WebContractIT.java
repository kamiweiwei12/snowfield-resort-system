package edu.sysu.snowfield.support;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 真实启动 Web 服务器的 HTTP 契约测试（对应任务板 INF-04）。
 *
 * <p>与 {@code GlobalExceptionHandlerTest} 的区别：那个是直接调用 handler 的单元测试，
 * 不经过 DispatcherServlet、序列化、过滤器链。本测试用 {@code RANDOM_PORT} 真实启动
 * 内嵌服务器并发起真实 HTTP 请求，证明响应信封在<b>线上格式</b>上与
 * {@code docs/API_CONTRACT.md} §1 一致。</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WebContractIT {

    // Boot 4 使用 Jackson 3（tools.jackson），API 与 Jackson 2 不同。
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @LocalServerPort
    private int port;

    private RestClient client() {
        // Boot 4 不再自动注册 RestClient.Builder，直接创建。
        return RestClient.create("http://127.0.0.1:" + port);
    }

    @Test
    void healthEndpointReturnsContractSuccessEnvelope() throws Exception {
        ResponseEntity<String> response = client()
                .get().uri("/api/v1/health")
                .retrieve().toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType())
                .isNotNull()
                .satisfies(type -> assertThat(type.isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue());

        JsonNode body = JSON.readTree(response.getBody());
        assertThat(body.get("success").asBoolean()).as("契约 §1：success 必须为 true").isTrue();
        assertThat(body.get("data").get("status").asText()).isEqualTo("UP");
        assertThat(body.get("traceId").asText()).as("契约 §1：必须携带 traceId").isNotBlank();
        assertThat(body.get("timestamp").asText())
                .as("契约 §1：timestamp 为带时区 ISO-8601")
                .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?\\+08:00");
    }

    @Test
    void traceIdIsDifferentPerRequest() throws Exception {
        JsonNode first = JSON.readTree(client().get().uri("/api/v1/health").retrieve().body(String.class));
        JsonNode second = JSON.readTree(client().get().uri("/api/v1/health").retrieve().body(String.class));

        assertThat(first.get("traceId").asText())
                .as("每次请求必须有独立 traceId，方可关联日志")
                .isNotEqualTo(second.get("traceId").asText());
    }

    @Test
    void unknownPathReturnsContractErrorEnvelope() throws Exception {
        assertThatThrownBy(() -> client().get().uri("/api/v1/does-not-exist").retrieve().toEntity(String.class))
                .isInstanceOf(HttpClientErrorException.class)
                .satisfies(thrown -> {
                    HttpClientErrorException ex = (HttpClientErrorException) thrown;
                    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    try {
                        JsonNode body = JSON.readTree(ex.getResponseBodyAsString());
                        assertThat(body.get("success").asBoolean()).isFalse();
                        assertThat(body.get("error").get("code").asText()).isEqualTo("RESOURCE_NOT_FOUND");
                        assertThat(body.get("error").get("fieldErrors").isArray()).isTrue();
                        assertThat(body.get("error").get("details").isObject()).isTrue();
                        assertThat(body.get("traceId").asText()).isNotBlank();
                    } catch (Exception parseFailure) {
                        throw new AssertionError("404 响应体不是合法 JSON：" + ex.getResponseBodyAsString(), parseFailure);
                    }
                });
    }

    @Test
    void errorResponseDoesNotLeakInternals() throws Exception {
        try {
            client().get().uri("/api/v1/does-not-exist").retrieve().toEntity(String.class);
            throw new AssertionError("预期 404");
        } catch (HttpClientErrorException ex) {
            String raw = ex.getResponseBodyAsString();
            // 安全要求：不泄漏堆栈、SQL、内部包名或文件路径。
            assertThat(raw).doesNotContain("java.", "org.springframework", "edu.sysu", "Exception", ".java", "SQL");
        }
    }

    @Test
    void openApiDocumentIsServed() throws Exception {
        ResponseEntity<String> response = client()
                .get().uri("/v3/api-docs")
                .retrieve().toEntity(String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode doc = JSON.readTree(response.getBody());
        assertThat(doc.get("openapi").asText()).as("必须是 OpenAPI 3 文档").startsWith("3.");
        assertThat(doc.get("paths").has("/api/v1/health"))
                .as("健康接口必须出现在 OpenAPI 文档中")
                .isTrue();
    }
}
