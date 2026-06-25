package com.cgrs.driver.service.impl;

import com.cgrs.driver.dao.PaymentOrderRepository;
import com.cgrs.driver.dao.UserRepository;
import com.cgrs.driver.dto.CreateOrderRequest;
import com.cgrs.driver.dto.PaymentCallbackRequest;
import com.cgrs.driver.model.PaymentOrder;
import com.cgrs.driver.model.User;
import com.cgrs.driver.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentOrderRepository orderRepo;

    @Autowired
    private UserRepository userRepo;

    private static final BigDecimal VIP_PRICE = new BigDecimal("19.90");
    private static final long VIP_QUOTA = 100L * 1024 * 1024 * 1024; // 100GB

    @Override
    public PaymentOrder createOrder(Long userId, CreateOrderRequest request) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        if (user.getStatus() != 1) {
            throw new RuntimeException("用户状态异常，无法购买");
        }

        PaymentOrder order = new PaymentOrder();
        order.setUserId(userId);
        order.setPlanType(0);       // 0 = VIP会员
        order.setAmount(VIP_PRICE);
        order.setStatus(0);
        return orderRepo.save(order);
    }

    @Override
    @Transactional
    public String processPaymentCallback(PaymentCallbackRequest request) {
        PaymentOrder order = orderRepo.findById(request.getOrderId())
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        if (order.getStatus() != 0) {
            return "订单已处理";
        }

        order.setStatus(1);
        order.setPayTime(LocalDateTime.now());

        User user = userRepo.findById(order.getUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime newExpire = now.plusMonths(1);
        if (user.getVipExpire() != null && user.getVipExpire().isAfter(now)) {
            newExpire = user.getVipExpire().plusMonths(1);   // 续费叠加
        }

        user.setRole(1);              // 成为付费用户
        user.setStorageQuota(VIP_QUOTA);
        user.setVipExpire(newExpire);
        user.setUsedStorage(user.getUsedStorage()); // 不变

        order.setEffectTime(now);
        order.setExpireTime(newExpire);

        userRepo.save(user);
        orderRepo.save(order);
        return "支付成功，VIP权益已生效";
    }

    @Override
    public List<PaymentOrder> getUserOrders(Long userId) {
        return orderRepo.findByUserIdOrderByCreateTimeDesc(userId);
    }
}
