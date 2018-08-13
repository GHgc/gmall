package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.UserInfo;

import java.util.List;

public interface UserInfoService {

    List<UserInfo> getUserInfoList();

    //登录方法
    public UserInfo login(UserInfo userInfo);

    //通过userInfo查询redis中是否有用户信息
    public UserInfo verify(String userId);
}
