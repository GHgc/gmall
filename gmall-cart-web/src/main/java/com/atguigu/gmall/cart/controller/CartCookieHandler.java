package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.config.CookieUtil;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@Component
public class CartCookieHandler {

    // 定义购物车名称
    private String cookieCartName = "CART";
    // 设置cookie 过期时间
    private int COOKIE_CART_MAXAGE=7*24*3600;
    @Reference
    private ManageService manageService;


    public void addToCart(HttpServletRequest request, HttpServletResponse response, String skuId, String userId, Integer skuNum){
//        判断下当前cookie中是否存在商品，如果有该商品，则数量+1
        //从cookie中获取商品数据
        String cartJson = CookieUtil.getCookieValue(request, cookieCartName, true);

//        严谨些，防止空指针错误
        List<CartInfo> cartInfoList = new ArrayList<>();
        //定义一个boolean 类型的变量，有该商品为true，无商品为 false
        boolean flag = false;
        if(cartJson!=null && "".equals(cartJson)){
            //将字符串转换成对象
            cartInfoList = JSON.parseArray(cartJson, CartInfo.class);
            //遍历集合
            for (CartInfo cartInfo : cartInfoList) {
                if(cartInfo.getSkuId().equals(skuId)){
                    cartInfo.setSkuNum(cartInfo.getSkuNum()+skuNum);
                    flag = true;
                    break;
                }
            }
        }

        //购物车中没有该商品的话
        if(!flag){
            //根据skuId查询商品信息，然后放到cartInfo

            //把商品信息取出来，新增到购物车
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            CartInfo cartInfo=new CartInfo();

            cartInfo.setSkuId(skuId);
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());

            cartInfo.setUserId(userId);
            cartInfo.setSkuNum(skuNum);

//            将该商品添加到购物车集合中
            cartInfoList.add(cartInfo);
        }
        //将 cartInfoList 转换成字符串
        String newCartJson = JSON.toJSONString(cartInfoList);
        //保存商品到cookie中
        CookieUtil.setCookie(request,response,cookieCartName,newCartJson,COOKIE_CART_MAXAGE,true);
    }

    //从 cookie   中获取购物车字符串
    public List<CartInfo> getCartList(HttpServletRequest request) {
        //从 cookie 中获取数据
        String cookieJson = CookieUtil.getCookieValue(request, cookieCartName, true);
        //将字符串转换成 cartInfo 对象
        List<CartInfo> cartInfoList = JSON.parseArray(cookieJson, CartInfo.class);
        return cartInfoList;
    }

    //删除 cookie 中购物车数据的方法
    public void deleteCartCookie(HttpServletRequest request,HttpServletResponse response){
        CookieUtil.deleteCookie(request,response,cookieCartName);
    }

    /**
     * 更改 cookie 中商品选中状态的方法
     * @param request
     * @param response
     * @param skuId
     * @param isChecked
     */
    public void checkCart(HttpServletRequest request, HttpServletResponse response, String skuId, String isChecked) {
        //  先获取 cookie 中的购物车数据
        // String cartJson = CookieUtil.getCookieValue(request,cookieCartName,true);
        //  直接调用之前写的方法,获取cookie中的购物车数据
        List<CartInfo> cartInfoList = getCartList(request);
        // 循环 cartInfoList 进行匹配,将 cookie 中 skuId 匹配的商品状态修改
        for (CartInfo cartInfo : cartInfoList) {
            // 如果 cookie 购物车中有当前的 skuId ,则将被选中的状态赋给该商品
            if(cartInfo.getSkuId().equals(skuId)){
                // 只给传递过来的参数 ischecked 为 1 的赋值, 0,1...// 判断0,1
                cartInfo.setIsChecked(isChecked);
            }
        }
        //将集合从新序列化到 cookieList 中
        String jsonString = JSON.toJSONString(cartInfoList);

        //重新放到 cookie 中
        CookieUtil.setCookie(request,response,cookieCartName,jsonString,COOKIE_CART_MAXAGE,true);
    }
}
