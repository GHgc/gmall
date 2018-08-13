package com.atguigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuSaleAttrValue;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;

@Controller
public class ItemController {

    @Reference
    ManageService manageService;

    @Reference
    ListService listService;

    /*https://item.jd.com/5089235.html https://item.jd.com/6784504.html springmvc: item.jd.com/{skuId}.html*/
    @RequestMapping("{skuId}.html")
    @LoginRequire(autoRedirect = true)
    public String item(@PathVariable(value = "skuId") String skuId , HttpServletRequest request){
        System.out.println(skuId);
        // 第一步，根据商品id进行查找数据，skuInfo,skuImage.
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        // 商品详情信息查询
        List<SpuSaleAttr> spuSaleAttrList = manageService.selectSpuSaleAttrListCheckBySku(skuInfo);

        // 根据spuId 查询销售属性值
        List<SkuSaleAttrValue> skuSaleAttrValueListBySpu = manageService.getSkuSaleAttrValueListBySpu(skuInfo.getSpuId());
        /*
        # 91|94 = 27   91|93 = 28
        # map.put("91|94",skuId);
        # {"91|94":27, "91|93":28}

         */
        String jsonKey = "";
        HashMap<String, String> map = new HashMap<>();
        for (int i = 0; i < skuSaleAttrValueListBySpu.size(); i++) {
            SkuSaleAttrValue skuSaleAttrValue = skuSaleAttrValueListBySpu.get(i);
            // 当jsonKey 不为空的时候
            if (jsonKey.length()!=0){
                jsonKey+="|";
            }
            jsonKey=jsonKey+skuSaleAttrValue.getSaleAttrValueId();
            // 什么时候停止当前的字符串拼接
            if ((i+1)==skuSaleAttrValueListBySpu.size() || !skuSaleAttrValue.getSkuId().equals(skuSaleAttrValueListBySpu.get(i+1).getSkuId())){
                map.put(jsonKey,skuSaleAttrValue.getSkuId());
                // 将字符串清空
                jsonKey="";
            }
        }
        // 将map集合转换成json对象
        String valuesSkuJson  = JSON.toJSONString(map);

        // 将字符串信息保存到后台，前台取得数据进行匹配 一组相关的spuId 对应的销售属性值以及skuId {"91|94":27, "91|93":28}
        request.setAttribute("valuesSkuJson",valuesSkuJson);
        // 保存信息
        request.setAttribute("spuSaleAttrList",spuSaleAttrList);
        // 保存skuInfo 信息
        request.setAttribute("skuInfo",skuInfo);

        //调用热度排名方法
        listService.incrHotScore(skuId);

        return "item";
    }
}
