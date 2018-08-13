package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.CartInfo;

import java.util.List;

public interface CartService {

    //添加商品到购物车
    public  void  addToCart(String skuId,String userId,Integer skuNum);

    //根据 userId 从数据库中获取购物车数据
    List<CartInfo> getCartList(String userId);

    //将缓存和数据库中的购物车数据进行合并
    //合并购物车
    List<CartInfo> mergeToCartList(List<CartInfo> cartListCK, String userId);

    //变更购物车选中的商品状态
    void checkCart(String skuId, String isChecked, String userId);

    //根据 userId 查询选中的商品列表
    List<CartInfo> getCartCheckedList(String userId);
}
