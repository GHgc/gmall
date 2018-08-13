package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.SpuImage;
import com.atguigu.gmall.bean.SpuInfo;
import com.atguigu.gmall.bean.SpuSaleAttrValue;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
public class SpuManageController {

    @Reference
    ManageService manageService;

    @RequestMapping(value = "/spuListPage")
    public String spuListPage(){
        return "spuListPage";
    }

    // http://localhost:8082/spuList?catalog3Id=61 根据三级分类Id，进行查询所有spuInfo信息
    @RequestMapping("spuList")
    @ResponseBody
    public List<SpuInfo> getSpuInfoList(String catalog3Id){
        SpuInfo spuInfo =new SpuInfo();
        spuInfo.setCatalog3Id(catalog3Id);
        List<SpuInfo> spuInfoList = manageService.getSpuInfoList(spuInfo);
        return spuInfoList;
    }

    @GetMapping(value = "spuImageList")
    @ResponseBody
    public List<SpuImage> getSpuImageList(String spuId){
        List<SpuImage> spuImageList = manageService.getSpuImageList(spuId);
        return spuImageList;
    };

    @RequestMapping("spuSaleAttrValueList")
    @ResponseBody
    public List<SpuSaleAttrValue> getSpuSaleAttrValueList(HttpServletRequest httpServletRequest){
        String spuId = httpServletRequest.getParameter("spuId");
        String saleAttrId = httpServletRequest.getParameter("saleAttrId");
        SpuSaleAttrValue spuSaleAttrValue=new SpuSaleAttrValue();
        spuSaleAttrValue.setSpuId(spuId);
        spuSaleAttrValue.setSaleAttrId(saleAttrId);
        List<SpuSaleAttrValue> spuSaleAttrValueList = manageService.getSpuSaleAttrValueList(spuSaleAttrValue);
        return spuSaleAttrValueList;

    }

}
