package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.config.HttpClientUtil;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.*;

@Service
public class OrderInfoServiceImpl implements OrderService {


    @Autowired
    OrderInfoMapper orderInfoMapper;
    @Autowired
    OrderDetailMapper orderDetailMapper;
    @Autowired
    RedisUtil redisUtil;

    @Override
    public String saveOrder(OrderInfo orderInfo) {
//        既要保存 orderInfo ,又要保存 orderDetail
        // 设置创建时间
        orderInfo.setCreateTime(new Date());
        // 设置失效时间   在创建时间的基础上再加一天
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE,1);
        orderInfo.setExpireTime(calendar.getTime());
        // 生成第三方支付编号
        String outTradeNo="ATGUIGU"+System.currentTimeMillis()+""+new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        // 调用通用 mapper 将 orderInfo 信息添加到数据库
        orderInfoMapper.insertSelective(orderInfo);

        //  插入订单详细信息
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insertSelective(orderDetail);
        }
        // 为了跳转到支付页面使用。支付会根据订单id进行支付。
        String orderId = orderInfo.getId();
        return orderId;
    }

    /**
     * 生成流水号
     */
    @Override
    public String getTradeNo(String userId){
        // 将生成的流水号存到 redis中
        Jedis jedis = redisUtil.getJedis();
        // 生成一个key
        String tradeNoKey="user:"+userId+":tradeCode";
        // 生成流水号
        String tradeNo = UUID.randomUUID().toString();
        // 将流水号保存到redis 中
        String result = jedis.setex(tradeNoKey, 10 * 60, tradeNo);
        if ("OK".equals(result)){
            return  tradeNo;
        }else {
            return null;
        }
    }
    //  验证流水号

    /**
     * @param userId 组成redis中的key
     * @param tradeCodeNo 传入进来的流水号
     * @return
     */
    public  boolean checkTradeCode(String userId,String tradeCodeNo){
        // 从前台页面传递过来的流水号,跟 redis 做比较,如果一样则返回 true ,否则false
        Jedis jedis = redisUtil.getJedis();
        // 生成一个 key
        String tradeNoKey="user:"+userId+":tradeCode";
        // 取出流水号
        String tradeNo = jedis.get(tradeNoKey);
        // 进行比较
        if (tradeCodeNo.equals(tradeNo)){
            return true;
        }else {
            return  false;
        }
    }

    @Override
    public boolean checkStock(String skuId, Integer skuNum) {
        // 如果库存系统返回1 则是true，0 是false；
        // 调用接口 ，借助工具类httpClientUtils
        String result = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum);

        if("1".equals(result)){
            return true;
        }else {
            return false;
        }
    }


    // 删除流水号    流水号对比后删除
    public void  delTradeCode(String userId){
        Jedis jedis = redisUtil.getJedis();
        // key
        String tradeNoKey="user:"+userId+":tradeCode";
        jedis.del(tradeNoKey);
        jedis.close();
    }

    @Override
    public OrderInfo getOrderInfo(String orderId) {
        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);
        return orderInfo;
    }

}
