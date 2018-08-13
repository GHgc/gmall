package com.atguigu.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.enums.PaymentStatus;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PaymentController {

    @Reference
    private OrderService orderService;

    @Reference
    private PaymentService paymentService;

    @Autowired
    private AlipayClient alipayClient;

    @RequestMapping("index")
    public String index(HttpServletRequest request){

        String orderId = request.getParameter("orderId");
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        //存 orderId
        request.setAttribute("orderId",orderId);
        //存 总价
        request.setAttribute("totalAmount",orderInfo.getTotalAmount());
        return "index";
    }

    @RequestMapping("alipay/submit")
    @ResponseBody
    public  String submitPayment(HttpServletRequest request, HttpServletResponse response){
//         第一件事情: 保存订单数据到数据库
        // 将数据插入 paymentInfo 表中
        // 获取订单 id
        String orderId = request.getParameter("orderId");
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);

        // 保存支付信息
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderId(orderId);
        // 保证交易的幂等性
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setCreateTime(new Date());

        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setSubject("欢迎来到支付宝支付页面,请放心付款,你想要的,我们都有！");
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);
        // 保存
        paymentService.savePaymentInfo(paymentInfo);

//         第二件事情: 生成页面给用户付款
        // 将 AlipayClient 的所有参数放到 配置文件中
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();//创建API对应的request
        // AlipayConfig.url
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);//在公共参数中设置回跳和通知地址

        // 组装成一个map，然后将map转换成字符串
        // 准备一个 map 集合,将生产付款页面的参数闯入到 alipayRequest 中
        Map<String,Object> map = new HashMap<>();
        map.put("out_trade_no",orderInfo.getOutTradeNo());
            // 销售产品码，与支付宝签约的产品码名称。 注：目前仅支持FAST_INSTANT_TRADE_PAY
        map.put("product_code","FAST_INSTANT_TRADE_PAY");
        map.put("total_amount",orderInfo.getTotalAmount());
        map.put("subject",paymentInfo.getSubject());
            // 将 map 转换成字符串
        String mapJson = JSON.toJSONString(map);
        alipayRequest.setBizContent(mapJson);
        // alipayRequest所有参数生产一个html 页面.
        String form="";
        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        response.setContentType("text/html;charset=utf-8");
        // 将from 显示到页面
        return form;
    }


    // 回调函数
    @RequestMapping(value = "alipay/callback/return",method = RequestMethod.GET)
    public String callbackReturn(){
        return "redirect://"+AlipayConfig.return_order_url;
    }

    // 异步回调
    @PostMapping(value = "/alipay/callback/notify")
    @ResponseBody
    public String paymentNotify(@RequestParam Map<String,String> paramMap, HttpServletRequest request) throws AlipayApiException {
        // paramsMap 将异步通知中收到的所有参数都存放到map中
        for (String map : paramMap.keySet()) {
            System.out.println("key="+map);
        }
        for (String value : paramMap.values()) {
            System.out.println("value="+value);
        }
        for (Map.Entry<String, String> stringStringEntry : paramMap.entrySet()) {
            System.out.println("stringStringEntry="+stringStringEntry);
        }

        boolean flag = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, "utf-8", AlipayConfig.sign_type); //调用SDK验证签名
        if(flag){
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure

            // 有一个状态 TRADE_SUCCESS 或 TRADE_FINISHED
            String trade_status = paramMap.get("trade_status");
            if("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)){
                // 交易状态成功   交易状态为 未支付 ,才能更改状态
                // 通过 out_trade_no 查询 paymentInfo
                String out_trade_no = paramMap.get("out_trade_no");
                // paymentService.getpaymentInfo(out_trade_no);
                PaymentInfo paymentInfo = new PaymentInfo();
                paymentInfo.setOutTradeNo(out_trade_no);
                PaymentInfo paymentInfoHas = paymentService.getpaymentInfo(paymentInfo);
                // 如果已支付或支付已关闭
                if (paymentInfoHas.getPaymentStatus()==PaymentStatus.PAID || paymentInfoHas.getPaymentStatus()==PaymentStatus.ClOSED){
                    return "fail";
                }else {
                    // 否则还未支付
                    // 修改 paymentInfo 的状态
                    PaymentInfo paymentInfoUpd = new PaymentInfo();
                    paymentInfoUpd.setPaymentStatus(PaymentStatus.PAID);
                    // 支付完成时间
                    paymentInfoUpd.setCallbackTime(new Date());
                    // 修改
                    paymentInfoUpd.setSubject(paramMap.toString());
                    // 根据 out_trade_no 修改对象
                    paymentService.updatePaymentInfo(out_trade_no,paymentInfoUpd);
                    // 成功!
                    return "success";
                }
            }

        }else{
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
        return "failure";
    }

}
