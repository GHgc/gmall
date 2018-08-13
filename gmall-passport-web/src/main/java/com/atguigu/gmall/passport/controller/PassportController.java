package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.passport.util.JwtUtil;
import com.atguigu.gmall.service.UserInfoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import javax.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    @Reference
    UserInfoService userInfoService;

//    取application配置文件中配置的key值
    @Value("${token.key}")
    private String key;

    //无论在什么页面，点击登录之后都会跳转到登录页面
    //所以，将originUrl保存到index之中
    @RequestMapping("index")
    public String index(HttpServletRequest request){
        String originUrl = request.getParameter("originUrl");
        //获取到后要保存下来，下次才能使用originUrl
        request.setAttribute("originUrl",originUrl);

        return "index";
    }

    @PostMapping(value = "login")
    @ResponseBody
    public String login(UserInfo userInfo,HttpServletRequest request){

        //接受到前台数据
        //调用登录方法验证登录
        UserInfo info = userInfoService.login(userInfo);
        //如果能查询得到，则登录成功
        //      jwt生成token 需要key，map，salt= ip,服务器的ip地址
        String ip = request.getHeader("X-forwarded-for");

        if(info!=null){
//            登录成功，要返回token
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("userId",info.getId());
            hashMap.put("nickName",info.getNickName());

            String token = JwtUtil.encode(key, hashMap, ip);
            System.out.println("token="+token);
            return token;
            //ctrl+shift+F12，代码区域最大化
        }else {
            return "fail";
        }
    }

    //做认证服务
    @RequestMapping(value = "verify")
    @ResponseBody
    public String verify(HttpServletRequest request){
        //获取token
        String token = request.getParameter("token");
        //从url中获取盐
        String currentIp = request.getParameter("currentIp");
        //调用工具类解密
        Map<String, Object> map = JwtUtil.decode(token, key, currentIp);
        if(map!=null){
            //从解密中获取用户id
            String userId = (String) map.get("userId");
            //验证redis中是否有用户，有则返回success，否则返回fail
            UserInfo userInfo= userInfoService.verify(userId);
            if(userInfo!=null){
                return "success";
            }else {
                return "fail";
            }
        }
        return "fail";
    }
}
