package com.atguigu.gmall.config;

import java.lang.annotation.*;

/**
 * 自定义注解，分辨方法是否需要登录
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginRequire {

//    添加一个默认属性值
    boolean autoRedirect() default true;
}
