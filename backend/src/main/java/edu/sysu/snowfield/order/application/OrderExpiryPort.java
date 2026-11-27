package edu.sysu.snowfield.order.application;

import java.util.List;

/** 过期扫描对 order 模块的公开端口。 */
public interface OrderExpiryPort {

    List<Long> lockDueOrderIds(int limit);

    void expireOrderAndItems(long orderId);
}
