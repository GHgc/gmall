package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.SpuSaleAttr;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface SpuSaleAttrMapper extends Mapper<SpuSaleAttr> {
    // 根据商品id进行查找销售属性
    List<SpuSaleAttr> selectSpuSaleAttrList(long spuId);
    // 通过spuId,以及skuId 查询销售属性值 ，是否有对应的商品
    List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(long skuId, long spuId);

}
