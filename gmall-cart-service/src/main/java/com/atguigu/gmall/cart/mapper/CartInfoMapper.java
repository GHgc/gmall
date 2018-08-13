package com.atguigu.gmall.cart.mapper;

import com.atguigu.gmall.bean.CartInfo;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface CartInfoMapper extends Mapper<CartInfo> {

    //自定义根据 userId 查询购物车列表数据
    List<CartInfo> selectCartListWithCurPrice(String userId);
}
