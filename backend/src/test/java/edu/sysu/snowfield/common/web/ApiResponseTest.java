package edu.sysu.snowfield.common.web;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 统一成功/错误响应信封的纯单元测试，不依赖 Spring 上下文。
 */
class ApiResponseTest {

    @Test
    void ok_setsSuccessAndData() {
        ApiResponse<Map<String, String>> resp = ApiResponse.ok(Map.of("status", "UP"));

        assertTrue(resp.success());
        assertEquals("UP", resp.data().get("status"));
        assertNotNull(resp.traceId());
        assertNotNull(resp.timestamp());
    }

    @Test
    void error_usesProvidedCodeAndMessage() {
        ErrorResponse resp = ErrorResponse.of("BOOKING_CONFLICT", "所选资源已被预订", null, null);

        assertEquals(false, resp.success());
        assertEquals("BOOKING_CONFLICT", resp.error().code());
        assertEquals("所选资源已被预订", resp.error().message());
        assertNotNull(resp.traceId());
        assertNotNull(resp.timestamp());
    }
}
