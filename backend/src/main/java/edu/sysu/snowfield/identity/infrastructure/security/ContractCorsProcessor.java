package edu.sysu.snowfield.identity.infrastructure.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.cors.DefaultCorsProcessor;

import edu.sysu.snowfield.common.web.ErrorCode;
import edu.sysu.snowfield.common.web.ErrorResponse;
import tools.jackson.databind.json.JsonMapper;

/** CORS 失败也使用项目统一错误信封，避免过滤器返回框架默认纯文本。 */
public class ContractCorsProcessor extends DefaultCorsProcessor {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Override
    protected void rejectRequest(ServerHttpResponse response) throws IOException {
        ErrorCode code = ErrorCode.ACCESS_DENIED;
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        ErrorResponse body = ErrorResponse.of(code.code(), code.defaultMessage(), List.of(), Map.of());
        response.getBody().write(JSON.writeValueAsString(body).getBytes(StandardCharsets.UTF_8));
    }
}
