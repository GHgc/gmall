package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.SkuSaleAttrValue;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface SkuSaleAttrValueMapper extends Mapper<SkuSaleAttrValue> {
    // 根据spuId 查询对应的销售属性值数据
    public List<SkuSaleAttrValue> selectSkuSaleAttrValueListBySpu (String spuId);
}
