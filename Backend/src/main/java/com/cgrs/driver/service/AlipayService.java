package com.cgrs.driver.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.cgrs.driver.config.AlipayConfig;
import com.cgrs.driver.dao.PaymentOrderRepository;
import com.cgrs.driver.model.PaymentOrder;
import lombok.extern.slf4j.Slf4j;          // 如果使用 Lombok
// import org.slf4j.Logger;               // 如果不使用 Lombok，则需要这两行
// import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j   // 如果项目已集成 Lombok，加上这个注解即可
@Service
public class AlipayService {

    @Autowired
    private AlipayClient alipayClient;

    @Autowired
    private AlipayConfig alipayConfig;

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    // 如果不使用 Lombok，请手动创建 Logger：
    // private static final Logger log = LoggerFactory.getLogger(AlipayService.class);

    public String createPayPage(PaymentOrder order) throws AlipayApiException {
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(alipayConfig.getNotifyUrl());
        request.setReturnUrl(alipayConfig.getReturnUrl());

        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        model.setOutTradeNo(order.getOrderId().toString());
        model.setTotalAmount(String.format("%.2f", order.getAmount().doubleValue()));
        model.setSubject("HDFS网盘VIP会员");
        model.setProductCode("FAST_INSTANT_TRADE_PAY");

        request.setBizModel(model);

        // 获取表单，只调用一次
        String formHtml = alipayClient.pageExecute(request).getBody();

        // 调试日志
        log.info("是否包含 biz_content: {}", formHtml.contains("biz_content"));

        int signIndex = formHtml.indexOf("name=\"sign\"");
        if (signIndex != -1) {
            int valueStart = formHtml.indexOf("value=\"", signIndex) + 7;
            int valueEnd = formHtml.indexOf("\"", valueStart);
            String signValue = formHtml.substring(valueStart, valueEnd);
            log.info("表单中的 sign 值: {}", signValue);
        }

        return formHtml;  // 只在此处返回一次
    }
}