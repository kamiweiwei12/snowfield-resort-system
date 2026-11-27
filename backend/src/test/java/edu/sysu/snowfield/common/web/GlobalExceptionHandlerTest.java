package edu.sysu.snowfield.common.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@link GlobalExceptionHandler} 的异常→错误响应映射单元测试（直接调用，不依赖 MockMvc）。
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void businessException_mapsToStableErrorCode() {
        ResponseEntity<ErrorResponse> resp = handler.handleBusiness(new BusinessException(ErrorCode.BOOKING_CONFLICT));

        assertEquals(HttpStatus.CONFLICT, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals("BOOKING_CONFLICT", resp.getBody().error().code());
        assertEquals("所选资源已被预订", resp.getBody().error().message());
    }

    @Test
    void businessException_customMessageOverridesDefault() {
        ResponseEntity<ErrorResponse> resp = handler.handleBusiness(new BusinessException(ErrorCode.BOOKING_CONFLICT, "已被他人抢订"));

        assertEquals("已被他人抢订", resp.getBody().error().message());
    }

    @Test
    void unexpectedException_mapsToInternalErrorWithoutDetails() {
        ResponseEntity<ErrorResponse> resp = handler.handleOther(new RuntimeException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        assertEquals("INTERNAL_ERROR", resp.getBody().error().code());
    }
}
