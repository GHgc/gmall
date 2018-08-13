package com.atguigu.gmall.manage.controller;


import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

@Controller
public class AttrManageController {

    @Reference
    ManageService manageService;

    @Reference
    ListService listService;

    /**
     * 获得一级分类
     */
    @RequestMapping(value = "getCatalog1")
    @ResponseBody
    public List<BaseCatalog1> getCatalog1(){
        List<BaseCatalog1> catalog1List  = manageService.getCatalog1();
        return catalog1List;
    }

    /**
     * 获得二级分类
     */
    @RequestMapping(value = "getCatalog2")
    @ResponseBody
    public List<BaseCatalog2> getCatalog2(String catalog1Id){
        List<BaseCatalog2> catalog2List = manageService.getCatalog2(catalog1Id);
        return catalog2List;
    }

    /**
     * 获得三级分类
     */
    @RequestMapping(value = "getCatalog3")
    @ResponseBody
    public List<BaseCatalog3> getCatalog3(String catalog2Id){
        List<BaseCatalog3> catalog3List = manageService.getCatalog3(catalog2Id);
        return catalog3List;
    }

    /**
     * 获得属性列表
     */
    @RequestMapping(value = "attrInfoList")
    @ResponseBody
    public List<BaseAttrInfo> getAttrInfoList(String catalog3Id){
        List<BaseAttrInfo> attrInfoList = manageService.getAttrList(catalog3Id);
        return attrInfoList;
    }

    /**
     * 保存属性信息
     */
    @PostMapping(value = "/saveAttrInfo")
    @ResponseBody
    public String saveAttrInfo(BaseAttrInfo baseAttrInfo){
        manageService.saveAttrInfo(baseAttrInfo);
        return "success";
    }

    /**
     * 编辑属性
     */
    @PostMapping(value = "/getAttrValueList")
    @ResponseBody
    public List<BaseAttrValue> getAttrValueList(String attrId){
        BaseAttrInfo attrInfo = manageService.getAttrInfo(attrId);
        return attrInfo.getAttrValueList();
    }


    /**
     * 删除属性
     */
    @RequestMapping(value = "/deleteAttrInfo")
    @ResponseBody
    public  String deleteAttrInfo(String attrId){
        manageService.deleteAttrInfo(attrId);
        return "success";
    }

    //商品上架管理器,根据skuId查询出skuInfo的属性，赋值给skuLsInfo
    @GetMapping(value = "onSale")
    @ResponseBody
    public void onSale(String skuId){
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        //进行属性拷贝
        SkuLsInfo skuLsInfo = new SkuLsInfo();

        //使用国内工具类，第一个参数是目标对象，第二个是源对象
        try {
            BeanUtils.copyProperties(skuLsInfo,skuInfo);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        //将数据保存到es之中
        listService.saveSkuInfo(skuLsInfo);
    }


}
