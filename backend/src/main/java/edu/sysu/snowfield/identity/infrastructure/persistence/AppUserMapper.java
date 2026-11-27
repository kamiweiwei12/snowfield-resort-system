package edu.sysu.snowfield.identity.infrastructure.persistence;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * {@code app_user} 表的 MyBatis Mapper（S2 连通性验证用最小集合）。
 *
 * <p>S2 阶段只验证「MyBatis 4.1.0 + Boot 4.1.3 + PostgreSQL」链路真实可用，
 * 完整身份模块的数据访问在 S4（认证接入）时按 {@code docs/ARCHITECTURE.md} 的
 * port/adapter 结构补齐。当前 SQL 全部使用 {@code #{}} 参数绑定（AGENTS.md §5）。</p>
 */
@Mapper
public interface AppUserMapper {

    @Select("""
            SELECT user_id, email, password_hash, display_name, phone, status, created_at, updated_at
            FROM app_user
            WHERE email = #{email}
            """)
    AppUserRecord findByEmail(@Param("email") String email);

    @Select("SELECT count(*) FROM app_user")
    long countAll();
}
