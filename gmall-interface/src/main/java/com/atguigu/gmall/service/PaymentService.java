package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PaymentInfo;

public interface PaymentService {

    // 保存数据
    void savePaymentInfo(PaymentInfo paymentInfo);

    // 根据 paymentInfo 查询相应的对象信息
    PaymentInfo getpaymentInfo(PaymentInfo paymentInfo);

    // 根据 out_trade_no 更新数据
    void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfoUpd);
}
