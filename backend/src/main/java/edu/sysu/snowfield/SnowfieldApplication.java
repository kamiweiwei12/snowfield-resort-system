package edu.sysu.snowfield;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 雪野营地四季度假综合运营与预订管理系统 - 后端入口。
 *
 * <p>本类只负责启动；Controller 只做协议、校验与权限入口，事务边界位于 Service。</p>
 */
@SpringBootApplication
@EnableScheduling
public class SnowfieldApplication {

    public static void main(String[] args) {
        SpringApplication.run(SnowfieldApplication.class, args);
    }
}
