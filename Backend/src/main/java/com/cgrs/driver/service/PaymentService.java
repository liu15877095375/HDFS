package com.cgrs.driver.service;

import com.cgrs.driver.dto.CreateOrderRequest;
import com.cgrs.driver.dto.PaymentCallbackRequest;
import com.cgrs.driver.model.PaymentOrder;

import java.util.List;

public interface PaymentService {
    PaymentOrder createOrder(Long userId, CreateOrderRequest request);
    String processPaymentCallback(PaymentCallbackRequest request);
    List<PaymentOrder> getUserOrders(Long userId);
}