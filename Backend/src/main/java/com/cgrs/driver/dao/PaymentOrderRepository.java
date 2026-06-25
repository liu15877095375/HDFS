package com.cgrs.driver.dao;

import com.cgrs.driver.model.PaymentOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    List<PaymentOrder> findByUserIdOrderByCreateTimeDesc(Long userId);
}