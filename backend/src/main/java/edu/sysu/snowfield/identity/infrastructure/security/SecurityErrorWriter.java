package edu.sysu.snowfield.identity.infrastructure.security;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import edu.sysu.snowfield.common.web.ErrorCode;
import edu.sysu.snowfield.common.web.ErrorResponse;
import tools.jackson.databind.json.JsonMapper;

/** 在 Security Filter Chain 内也返回统一错误信封，不把框架异常或令牌细节泄漏给客户端。 */
@Component
public class SecurityErrorWriter implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        ErrorCode code;
        if (exception instanceof InvalidBearerTokenException) {
            code = containsExpired(exception) ? ErrorCode.TOKEN_EXPIRED : ErrorCode.TOKEN_INVALID;
        } else {
            code = ErrorCode.AUTH_REQUIRED;
        }
        write(response, code);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        write(response, ErrorCode.ACCESS_DENIED);
    }

    private static boolean containsExpired(Throwable throwable) {
        for (Throwable cursor = throwable; cursor != null; cursor = cursor.getCause()) {
            String message = cursor.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("expired")) {
                return true;
            }
        }
        return false;
    }

    private static void write(HttpServletResponse response, ErrorCode code) throws IOException {
        response.setStatus(code.httpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ErrorResponse body = ErrorResponse.of(code.code(), code.defaultMessage(), List.of(), Map.of());
        response.getWriter().write(JSON.writeValueAsString(body));
    }
}
