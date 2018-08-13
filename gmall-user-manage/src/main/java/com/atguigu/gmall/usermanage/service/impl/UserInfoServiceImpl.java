package com.atguigu.gmall.usermanage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.service.UserInfoService;
import com.atguigu.gmall.usermanage.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import redis.clients.jedis.Jedis;

import java.util.List;

@Service
public class UserInfoServiceImpl implements UserInfoService {

    @Autowired
    UserInfoMapper userInfoMapper;
    @Autowired
    RedisUtil redisUtil;

    public String userKey_prefix="user:";
    public String userinfoKey_suffix=":info";
    public int userKey_timeOut=60*60;



    @Override
    public List<UserInfo> getUserInfoList() {
        List<UserInfo> userInfoList = userInfoMapper.selectAll();
        return userInfoList;
    }

    //登录方法
    @Override
    public UserInfo login(UserInfo userInfo) {
        /*调用工具类，对密码进行md5密文处理*/
        //userInfo.getPasswd().getBytes()   获取明文密码，以byte数组方式放入
        userInfo.setPasswd(DigestUtils.md5DigestAsHex(userInfo.getPasswd().getBytes()));
        UserInfo info = userInfoMapper.selectOne(userInfo);
//      当info不为空的时候，将用户信息放到redis之中
        if(info!=null){
            //取得jedis
            Jedis jedis = redisUtil.getJedis();
//            定义key，之前定义了常量，直接拿过来用即可
            // 定义key user:UserId:info ,sku:skuId:info user:1:info
            String userKey = userKey_prefix+info.getId()+userinfoKey_suffix;
            //将数据放入redis
                //将info对象转换为json字符串，保存进redis之中
            String userJson = JSON.toJSONString(info);
            //ctrl+P 显示方法的形参
            jedis.setex(userKey,userKey_timeOut,userJson);
            return info;
        }else {
            return null;
        }
    }

    //验证redis中是否有用户，有则返回success，否则返回fail
    @Override
    public UserInfo verify(String userId) {
        //获取jedis对象
        Jedis jedis = redisUtil.getJedis();
        //key   user:userId:info
        String userKey = userKey_prefix+userId+userinfoKey_suffix;
        String userJson = jedis.get(userKey);
        if(userJson!=null && !"".equals(userJson)){
            //将userJson转换为UserInfo对象返回
            UserInfo userInfo = JSON.parseObject(userJson, UserInfo.class);
            return userInfo;
        }
        return null;
    }
}
