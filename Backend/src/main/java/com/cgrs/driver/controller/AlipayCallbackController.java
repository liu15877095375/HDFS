package com.cgrs.driver.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.cgrs.driver.config.AlipayConfig;
import com.cgrs.driver.model.PaymentOrder;
import com.cgrs.driver.dao.PaymentOrderRepository;
import com.cgrs.driver.service.PaymentService; // 引入原有的支付服务
import com.cgrs.driver.dto.PaymentCallbackRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@RestController
public class AlipayCallbackController {

    @Autowired
    private AlipayConfig alipayConfig;
    @Autowired
    private PaymentOrderRepository orderRepository;
    @Autowired
    private PaymentService paymentService; // 注入原有的支付服务

    @PostMapping("/api/payment/alipay/notify")
    public String alipayNotify(HttpServletRequest request) throws AlipayApiException {
        // 1. 获取支付宝POST过来的反馈信息
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (Iterator<String> iter = requestParams.keySet().iterator(); iter.hasNext();) {
            String name = iter.next();
            String[] values = requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            params.put(name, valueStr);
        }

        // 2. ！！！重要：验证签名，确保请求来源于支付宝官方！！！
        boolean signVerified = AlipaySignature.rsaCheckV1(params,
                alipayConfig.getAlipayPublicKey(),
                "UTF-8",
                "RSA2");

        if (signVerified) {
            // 3. 获取订单信息
            String outTradeNo = params.get("out_trade_no");
            String tradeStatus = params.get("trade_status");

            // 4. 检查交易状态是否为成功
            if ("TRADE_SUCCESS".equals(tradeStatus)) {
                PaymentOrder order = orderRepository.findById(Long.valueOf(outTradeNo)).orElse(null);
                if (order != null && order.getStatus() == 0) {
                    // 调用原有的支付成功处理逻辑（修改用户角色、配额等）
                    PaymentCallbackRequest callbackRequest = new PaymentCallbackRequest();
                    callbackRequest.setOrderId(order.getOrderId());
                    paymentService.processPaymentCallback(callbackRequest);
                }
            }
            // 处理完成后，必须返回 success，否则支付宝会重复通知
            return "success";
        } else {
            // 签名验证失败，记录日志并返回失败
            return "failure";
        }
    }
}
