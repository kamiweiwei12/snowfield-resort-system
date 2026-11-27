package edu.sysu.snowfield.identity.application;

import java.time.OffsetDateTime;
import java.util.Set;

/** identity 用例签发短期访问令牌的输出端口；不向 application 层泄漏 JWT 框架类型。 */
public interface AccessTokenPort {

    IssuedAccessToken issue(long userId, Set<String> roles);

    record IssuedAccessToken(String accessToken, OffsetDateTime expiresAt) {
    }
}
