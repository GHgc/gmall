package com.atguigu.gmall.config;

import com.alibaba.fastjson.JSON;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
//        先获取token
        String token = request.getParameter("newToken");
        //判断token是否为null
        if(token!=null){
            //就把token放到cookie中
            CookieUtil.setCookie(request,response,"token",token,WebConst.COOKIE_MAXAGE,false);
        }
        //既然获取了token，就说明已经登录了，但token为null，说明是从其他页面中过来的
        //则从cookie中获取token
        if (token==null){
             token = CookieUtil.getCookieValue(request, "token", false);
        }
        //然后判断是否成功取得token
        //取得成功后进行解密，并将用户的昵称保存到作用域中，用于放到页面中显示
        if(token!=null){
            //解密token
            Map map = getUserMapByToken(token);
            String nickName = (String) map.get("nickName");
            //放到作用域中
            request.setAttribute("nickName",nickName);
        }
        // 获取到当前控制器的方法的注解
        HandlerMethod  handlerMethod  = (HandlerMethod)handler;
        // 如果该类上没有LoginRequire,那么loginRequireAnnotation 返回null
        // 要想获取到userId ，则在方法上需要添加LoginRequire注解！
        LoginRequire loginRequireAnnotation = handlerMethod.getMethodAnnotation(LoginRequire.class);
        if(loginRequireAnnotation!=null){
            //调用认证方法，链接为        http://passprot.atguigu.com/verify?token=xxxx&currentIp=xxx
            //WebConst.VERIFY_ADDRESS   为http://passport.atguigu.com/verify
            //需要获取ip    192.168.154.1
            String remoteAddr = request.getHeader("x-forwarded-for");
            //使用HttpClientUtil工具类，调用远程的认证方法
            String result = HttpClientUtil.doGet(WebConst.VERIFY_ADDRESS + "?token=" + token + "&currentIp=" + remoteAddr);
//            进行判断
            if("success".equals(result)){
                //成功的话，保存useId，写购物车时使用
                Map map = getUserMapByToken(token);
                String userId = (String) map.get("userId");
                request.setAttribute("userId",userId);
                return true;
            }else {
                //严谨点，继续判断,如果注解的值为true,则表示该方法需要登录
                if(loginRequireAnnotation.autoRedirect()){
                  //获取url进行拼接
                    String requestURL = request.getRequestURL().toString();
                    //http%3A%2F%2Fitem.gmall.com%2F28.html 转换成utf-8
                    String encodeURL = URLEncoder.encode(requestURL, "UTF-8");
                    //把链接发送过去让用户进行登录
                        //拼接链接,并发送回去
                    response.sendRedirect(WebConst.LOGIN_ADDRESS +"?originUrl="+encodeURL);
                    return false;
                }
            }

        }


        return true;
    }

    /**解密token的方法
     * token分为三部分
     * eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6IkFkbWluaXN0cmF0b3IiLCJ1c2VySWQiOiIyIn0.WUvbFvXQnTMBGNyHWT-DE41MR9cn7c_W1oAtDAzb7VU
     */
    private Map getUserMapByToken(String token) {
        //使用工具类，将token进行分割
        String tokenUserInfo = StringUtils.substringBetween(token,".");
        //使用Base64UrlCodec进行解密
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();
        byte[] decode = base64UrlCodec.decode(tokenUserInfo);
        //将字节数组转换成字符串
        String newUserInfo =null;
        try {
             newUserInfo = new String(decode,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
//        将字符串转换成对象
        Map map = JSON.parseObject(newUserInfo, Map.class);
        return map;
    }
}
