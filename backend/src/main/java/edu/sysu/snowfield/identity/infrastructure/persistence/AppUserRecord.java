package edu.sysu.snowfield.identity.infrastructure.persistence;

import java.time.OffsetDateTime;

/**
 * {@code app_user} 表的 MyBatis 行对象（Record）。
 *
 * <p>架构约束（docs/ARCHITECTURE.md §6）：Record 只存在于
 * {@code <module>/infrastructure/persistence} 中，不得泄漏到 domain/api 层。</p>
 */
public record AppUserRecord(
        Long userId,
        String email,
        String passwordHash,
        String displayName,
        String phone,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
}
