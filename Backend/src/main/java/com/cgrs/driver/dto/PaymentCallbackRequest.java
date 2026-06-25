package com.cgrs.driver.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentCallbackRequest {
    @NotNull(message = "订单ID不能为空")
    private Long orderId;
    // 实际支付回调会有更多字段（交易号等），此处仅模拟
}