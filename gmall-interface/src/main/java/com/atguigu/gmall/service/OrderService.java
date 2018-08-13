package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OrderInfo;

public interface OrderService {
    // 保存订单
    public  String  saveOrder(OrderInfo orderInfo);
    // 生成流水号
    public String getTradeNo(String userId);

    //验证流水号
    boolean checkTradeCode(String userId, String tradeNo);

    //库存验证
    boolean checkStock(String skuId, Integer skuNum);

    //删除流水号
    void delTradeCode(String userId);

    //通过 orderId 查询 orderInfo
    OrderInfo getOrderInfo(String orderId);
}
