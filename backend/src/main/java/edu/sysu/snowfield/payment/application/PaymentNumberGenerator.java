package edu.sysu.snowfield.payment.application;

import java.util.UUID;

import org.springframework.stereotype.Component;

/** 生成数据库唯一的模拟支付编号。 */
@Component
public class PaymentNumberGenerator {

    public String next() {
        return "PAY" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
    }
}
