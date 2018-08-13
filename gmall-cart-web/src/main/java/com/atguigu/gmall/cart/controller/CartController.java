package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
public class CartController {

    @Reference
    private CartService cartService;

    @Reference
    private ManageService manageService;

    @Autowired
    private CartCookieHandler cartCookieHandler;

    /**
     * gmall-item-web 中的页面中，找到加入购物车的 addToCart
     */
    @RequestMapping(value = "addToCart",method = RequestMethod.POST)
    @LoginRequire(autoRedirect = false) //没有此注解，获取不到
    public String addToCart(HttpServletRequest request, HttpServletResponse response){
//        从页面中获取数据
        String skuId = request.getParameter("skuId");
        String skuNum = request.getParameter("skuNum");
        //gmall-web-util 的 AuthInterceptor 中，将 userId 放入了attribute 现在拿过来使用
        String userId = (String) request.getAttribute("userId");

        //判断当前用户是否处于登录状态
        if(userId!=null){
            //登录状态
            cartService.addToCart(skuId,userId,Integer.parseInt(skuNum));
        }else {
            //没有登录的状态
            cartCookieHandler.addToCart(request,response,skuId,userId,Integer.parseInt(skuNum));
        }
        //保存skuInfo信息   通过skuId查询
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        request.setAttribute("skuInfo",skuInfo);
        //保存商品数量
        request.setAttribute("skuNum",skuNum);
      return "success";
    }

    //展示购物车列表页面,合并购物车
    @RequestMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public String cartList(HttpServletRequest request,HttpServletResponse response){
        //取得    userId
        String userId = (String) request.getAttribute("userId");
        /**
         * 如果登录了，就合并购物车集合，将 cookie 中的和 db 中的购物车商品合并
         * 如果没有登录，则只取 cookie 中的数据返回页面
         */
        if(userId!=null){
            //已登录，进行购物车合并
            List<CartInfo> cartListCK = cartCookieHandler.getCartList(request);
            List<CartInfo> cartInfoList = null;
            if(cartListCK!=null && cartListCK.size()>0){
                //确认 cookie 购物车有数据，进行合并
                cartInfoList = cartService.mergeToCartList(cartListCK,userId);
                // 将cookie 中的购物车进行删除
                cartCookieHandler.deleteCartCookie(request,response);
            }else{
                // 从数据库取得, redis中 user:userId:cart
                cartInfoList = cartService.getCartList(userId);
            }
            // 从redis中取得，或者从数据库中
            List<CartInfo> cartList = cartService.getCartList(userId);
            request.setAttribute("cartList",cartList);
        }else {
            //未登录,则从 cookie 购物车中取出数据进行显示
            List<CartInfo> cartList = cartCookieHandler.getCartList(request);
            request.setAttribute("cartList",cartList);
        }
        return "cartList";
    }


    @RequestMapping("checkCart")
    @ResponseBody
    @LoginRequire(autoRedirect = false)
    public void checkCart(HttpServletRequest request,HttpServletResponse response){
        //获取传入的参数
        String isChecked = request.getParameter("isChecked");
        String skuId = request.getParameter("skuId");
        //获取userId
        String userId = (String) request.getAttribute("userId");
        //判断用户是否登录
        if (userId!=null){
            // mysql-redis
            //已登录   则修改数据库中的商品状态
            cartService.checkCart(skuId,isChecked,userId);
        }else{
            // cookie request,response,
            //未登录,  则在cookie中修改商品状态
            cartCookieHandler.checkCart(request,response,skuId,isChecked);
        }
    }

    /**
     * 结算
     */
    @RequestMapping("toTrade")
    @LoginRequire(autoRedirect = true)  //登录后才可以结算
    public String toTrade(HttpServletRequest request,HttpServletResponse response){
        //点击结算的时候,还需要进行一次购物车合并
        //useId 在拦截器中取得,也可以在缓存中取得   user:userId:info-db
        String userId = (String) request.getAttribute("userId");
        List<CartInfo> cartListCK = cartCookieHandler.getCartList(request);
        if(userId!=null){
            //直接合并
            cartService.mergeToCartList(cartListCK,userId);
            // 将cookie 中的购物车进行删除
            cartCookieHandler.deleteCartCookie(request,response);
        }

        //重定向到一个控制器
        return "redirect://order.gmall.com/trade";
    }

}
