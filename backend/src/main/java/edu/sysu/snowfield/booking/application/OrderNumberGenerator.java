package edu.sysu.snowfield.booking.application;

import org.springframework.stereotype.Component;

/**
 * 订单号生成器。
 *
 * <p>当前实现：`SO` + 毫秒时间戳 + 进程内自增序号，保证单进程内唯一；
 * 跨进程唯一性由数据库 `uq_order_no` 唯一约束兜底（插入冲突即抛错，不做静默重试）。
 * 若将来多实例部署，应替换为数据库序列或雪花算法，届时需重新评估。</p>
 */
@Component
public class OrderNumberGenerator {

    private final java.util.concurrent.atomic.AtomicInteger sequence =
            new java.util.concurrent.atomic.AtomicInteger(0);

    public String next() {
        long millis = System.currentTimeMillis();
        int seq = Math.floorMod(sequence.getAndIncrement(), 1000);
        return "SO%013d%03d".formatted(millis, seq);
    }
}
