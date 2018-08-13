package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;

public interface ListService {

    //保存SkuLsInfo到es之中
    public void saveSkuInfo(SkuLsInfo skuLsInfo);

    //编写dsl语句查询返回的结果
    public SkuLsResult search(SkuLsParams skuLsParams);

    //热度排行
    public void incrHotScore(String skuId);

}
