package com.atguigu.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {

    @Reference
    private ListService listService;

    @Reference
    private ManageService manageService;

    @RequestMapping("list.html")
    public String list(SkuLsParams skuLsParams, Model model){

        // 设置每页显示的条数
        skuLsParams.setPageSize(2);

        // 调用listService
        SkuLsResult skuLsResult = listService.search(skuLsParams);
        // 将对象转成json字符串
        String listJson = JSON.toJSONString(skuLsResult);

        //在页面应该显示skuLsInfo属性的信息
        List<SkuLsInfo> skuLsInfoList = skuLsResult.getSkuLsInfoList();
        //从es中查询出来skuInfo信息，页面在页面显示
        model.addAttribute("skuLsInfoList",skuLsInfoList);

        //返回值的时候，有了平台属性值的集合
        List<String> attrValueIdList = skuLsResult.getAttrValueIdList();
        //根据把所有的属性值查出来，然后循环其中的属性值信息
            //后台写一个方法，根据平台属性值的集合查出平台属性信息
       List<BaseAttrInfo> attrList = manageService.getAttrList(attrValueIdList);
       model.addAttribute("attrList",attrList);

        // 点击平台属性值的时候，将url添加上平台属性值的参数 看作一个字段 http://list.gmall.com/list.html?catalog3Id=61
        //http://list.gmall.com/list.html?catalog3Id=61&valueId=80&valueId=83

//        面包屑：      平台属性：平台属性值
        //        将面包屑放到一个集合列表中
        List<BaseAttrValue> baseAttrValuesList = new ArrayList<>();
//        BaseAttrValue.setvalueName(“平台属性：平台属性值”)


        //做拼接
        String urlParam = makeUrlParam(skuLsParams);

        //拼接去重
        for (Iterator<BaseAttrInfo> iterator = attrList.iterator(); iterator.hasNext(); ) {
            BaseAttrInfo baseAttrInfo = iterator.next();

            //取得数据库中的平台属性id
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
              /*数据库的Id，跟skuLsParam 做比较，如果有相同的，则移除*/
            for (BaseAttrValue baseAttrValue : attrValueList) {
//                需要重新赋值一个urlParam
                baseAttrValue.setUrlParam(urlParam);
                if(skuLsParams.getValueId()!=null && skuLsParams.getValueId().length>0){
//                    循环做匹配，移除数据
                    for (String valueId : skuLsParams.getValueId()) {
//                            如果平台属性值Id相同则将数据移除
                        if(baseAttrValue.getId().equals(valueId)){
                            iterator.remove();

//                            该循环中有平台属性和平台属性值。创建一个被选中的平台属性值对象
                            BaseAttrValue baseAttrValueselected = new BaseAttrValue();
                            //平台属性：平台属性值        就是valueName
                            baseAttrValueselected.setValueName(baseAttrInfo.getAttrName()+":"+baseAttrValue.getValueName());

                            //面包屑去重
                            /*下面的方法进行了修改，当id相同时，会去重*/
                            String makeUrlParam = makeUrlParam(skuLsParams, valueId);
                            baseAttrValueselected.setUrlParam(makeUrlParam);


                            //将面包屑放入当前的集合之中
                            baseAttrValuesList.add(baseAttrValueselected);
                        }
                    }
                }
            }


        }


        model.addAttribute("baseAttrValuesList",baseAttrValuesList);

        //保存关键字
        model.addAttribute("keyword",skuLsParams.getKeyword());

        //保存url
        model.addAttribute("urlParam",urlParam);

        // 保存一下totalPages，pageNo 总条数是通过es
//        int pages = (int) ((skuLsResult.getTotal() +(skuLsParams.getPageSize()-1))/skuLsParams.getPageSize());
        model.addAttribute("totalPages",skuLsResult.getTotalPages());
        model.addAttribute("pageNo",skuLsParams.getPageNo());
        return "list";
    }

    //制作url拼接

    /**
     * @param skuLsParams   url中的参数
     * @param excludeValueIds   通过点击平台属性值【点击面包屑的时传入的平台属性值Id】
     * @return
     */
    public String makeUrlParam(SkuLsParams skuLsParams,String ...excludeValueIds){

        //先声明一个参数
        String urlParam="";
        //当keyword不为空的时候，拼接参数
        if(skuLsParams.getKeyword()!=null && skuLsParams.getKeyword().length()>0){
            urlParam+="keyword="+skuLsParams.getKeyword();
        }

        //  拼接三级分类Id
        // 当keyword 不为空的时候，
        // 拼接参数 http://list.gmall.com/list.html?keyword="小米"&catalog3Id=61
        if(skuLsParams.getCatalog3Id()!=null && skuLsParams.getCatalog3Id().length()>0){
            if(urlParam.length()>0){
                urlParam+="&";
            }
            urlParam+="catalog3Id="+skuLsParams.getCatalog3Id();
        }
        // 拼接属性值Id
        // 拼接参数 http://list.gmall.com/list.html?keyword="小米"&catalog3Id=61&valaueId=80&valueId=83
        if(skuLsParams.getValueId()!=null&&skuLsParams.getValueId().length>0){
            for (int i = 0; i < skuLsParams.getValueId().length; i++) {
                String valueId = skuLsParams.getValueId()[i];

//                判断两个平台属性值的id是否相等
                if(excludeValueIds!=null && excludeValueIds.length>0){
                    //在excludeValueIds中只取第一个值即可
                    String excludeValueId = excludeValueIds[0];
                    if(excludeValueId.equals(valueId)){
                        //相等时，此平台属性值的id不拼接
                        continue;
                    }
                }

                if(urlParam.length()>0){
                    urlParam+="&";
                }
                urlParam+="valueId="+valueId;
            }
        }

        return urlParam;
    }
}
