package com.cgrs.driver.controller;

import com.alipay.api.AlipayApiException;
import com.cgrs.driver.dto.CreateOrderRequest;
import com.cgrs.driver.dto.PaymentCallbackRequest;
import com.cgrs.driver.model.PaymentOrder;
import com.cgrs.driver.service.AlipayService;
import com.cgrs.driver.service.PaymentService;
import com.cgrs.driver.utils.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private AlipayService alipayService;

    @Autowired
    private JwtUtil jwtUtil;

    private Long getUserIdFromToken(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.getUserIdFromToken(token);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createOrder(@RequestHeader("Authorization") String auth,
                                         @Valid @RequestBody CreateOrderRequest request) {
        Long userId = getUserIdFromToken(auth);
        PaymentOrder order = paymentService.createOrder(userId, request);
        Map<String, Object> res = new HashMap<>();
        res.put("orderId", order.getOrderId());
        res.put("amount", order.getAmount());
        return ResponseEntity.ok(res);
    }

    /**
     * 创建支付宝支付页面
     * 返回HTML表单，用于在新窗口打开支付宝沙箱支付
     */
    @PostMapping(value = "/alipay-page", produces = MediaType.TEXT_HTML_VALUE)
    public String createAlipayPage(@RequestHeader("Authorization") String auth,
                                    @Valid @RequestBody CreateOrderRequest request) throws AlipayApiException {
        Long userId = getUserIdFromToken(auth);
        PaymentOrder order = paymentService.createOrder(userId, request);
        // 生成支付宝支付页面HTML
        return alipayService.createPayPage(order);
    }

    @PostMapping("/callback")
    public ResponseEntity<?> paymentCallback(@RequestHeader("Authorization") String auth,
                                             @Valid @RequestBody PaymentCallbackRequest request) {
        // 实际生产中需验证订单归属，此处略
        String result = paymentService.processPaymentCallback(request);
        return ResponseEntity.ok(Map.of("message", result));
    }

    @GetMapping("/orders")
    public ResponseEntity<?> myOrders(@RequestHeader("Authorization") String auth) {
        Long userId = getUserIdFromToken(auth);
        List<PaymentOrder> orders = paymentService.getUserOrders(userId);
        return ResponseEntity.ok(orders);
    }
}
