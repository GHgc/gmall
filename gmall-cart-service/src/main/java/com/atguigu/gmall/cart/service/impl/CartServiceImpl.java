package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.cart.constant.CartConst;
import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.*;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Reference
    private ManageService manageService;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {
        //添加购物车，如何显示
        CartInfo cartInfo = new CartInfo();
        cartInfo.setSkuId(skuId);
        cartInfo.setUserId(userId);
        //判断当前的购物车中是否有该商品，如果有该商品，则数量+skuNum
        CartInfo cartInfoExist = cartInfoMapper.selectOne(cartInfo);
        if(cartInfoExist!=null){
            //当前购物车有该商品时，该商品数量+1
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum()+skuNum);
            //将商品数量信息更新到数据库
            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExist);
        }else {
//          当前购物车中没有该商品，所以要新增cartInfo
            //新增cartInfo中的 price、skuName、imgUrl 数据要从skuInfo中获取
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            CartInfo newCartInfo = new CartInfo();

            newCartInfo.setUserId(userId);
            newCartInfo.setSkuId(skuId);
            newCartInfo.setCartPrice(skuInfo.getPrice());
            newCartInfo.setSkuPrice(skuInfo.getPrice());
            newCartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            newCartInfo.setSkuName(skuInfo.getSkuName());
            newCartInfo.setSkuNum(skuNum);

            cartInfoMapper.insertSelective(newCartInfo);
            //说明当前购物车中没有该商品，则cartInfoExist是空的，所以借助当前对象
            cartInfoExist = newCartInfo;
        }

//      将购物车数据放到缓存中 使用hset(key,field,value) ;   key=user:userId:cart
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        //将cart数据转换成json字符串放到缓存中
        Jedis jedis = redisUtil.getJedis();
        String cartInfoJSON = JSON.toJSONString(cartInfoExist);
        jedis.hset(userCartKey,skuId,cartInfoJSON);
        //更新购物车的过期时间，利用user的过期时间 user:2:info
        String userKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USERINFOKEY_SUFFIX;
        //用ttl取得当前用户登录key的过期时间
        Long ttl = jedis.ttl(userKey);
        jedis.expire(userCartKey,ttl.intValue());
        jedis.close();
    }

    @Override
    public List<CartInfo> getCartList(String userId) {
        List<CartInfo> cartInfoList = new ArrayList<>();
        //定义 redis 的 key    user：userId:cart
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        //在 redis 中用 hget(key,field) 取得
        Jedis jedis = redisUtil.getJedis();
        List<String> cartJsons = jedis.hvals(userCartKey);

        //将 redis 中的数据，添加到 List<CartInfo> cartInfoList 中
        if(cartJsons!=null && cartJsons.size()>0){
            for (String cartJson : cartJsons) {
                //cartJson 是字符串，转换成对象，放到 cartInfoList 中
                CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
                cartInfoList.add(cartInfo);
            }
            //redis的hash结构是无序的，要进行排序（可以用时间戳或者主键id，倒序排序）
            //不排序的话，每次显示出来的商品顺序不同
            // 集合的排序功能
            cartInfoList.sort(new Comparator<CartInfo>() {
                //自定义比较，外部比较器
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    //String 中常用的七个方法
                    return o1.getId().compareTo(o2.getId());
                }
            });
            return cartInfoList;
        }else {
            //当 redis 缓存中没有数据，则从 db 数据库中获取数据
            List<CartInfo> cartInfoListCache = loadCartCache(userId);
            return cartInfoListCache;
        }

    }

    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartListCK, String userId) {
//        先从数据库中查到数据
        List<CartInfo> cartInfoListDB = cartInfoMapper.selectCartListWithCurPrice(userId);

        List<CartInfo> cartInfoList = null;
        if(cartListCK!=null && cartListCK.size()>0){
            // 循环CK ,准备启用boolean 类型变量，记录是否有配备数据，如果有，则赋值为true，没有则进行插入数据库
            for (CartInfo cartInfoCK : cartListCK) {
                boolean isMatch = false;
                for (CartInfo cartInfoDB : cartInfoListDB) {
                    // 当数据库和cookie中都有该商品时，该商品数量相加
                    cartInfoDB.setSkuNum(cartInfoCK.getSkuNum()+cartInfoDB.getSkuNum());
                    cartInfoMapper.updateByPrimaryKeySelective(cartInfoDB);
                    isMatch =true;
                }
                // 没有情况下，插入数据库
                // 再ck循环过程中没有找到匹配数据，则进行插入数据库
                if (!isMatch){
                    // cookie'中没有userId ，因为cookie中存放的是未登录的数据，所以再插入的时候，将cookie对象中的cartInfo中userId赋值
                    cartInfoCK.setUserId(userId);
                    cartInfoMapper.insertSelective(cartInfoCK);
                }
            }
            // 将合并之后的数据，通过userId,查询出来从新放入缓存
            cartInfoList = loadCartCache(userId);
            // 做一个被选中商品的合并,会将 cookieList 中被选中的商品丢失
            for (CartInfo cartInfo : cartInfoList) {
                // cookie 中的数据
                for (CartInfo info : cartListCK) {
                    // 27 cartInfo = 27 info
                    if(cartInfo.getSkuId().equals(info.getSkuId())){
                        // "isChecked = 1"
                        if(info.getIsChecked().equals("1")){
                            // user:userId:checked 更新一下
                            cartInfo.setIsChecked(info.getIsChecked());
                            checkCart(info.getSkuId(),info.getIsChecked(),userId);
                        }
                    }
                }
            }
        }



        return cartInfoList;
    }

    @Override
    public void checkCart(String skuId, String isChecked, String userId) {
        /**
         * 根据商品 skuId 从 redis 中取出购物车数据;
         * 遍历循环将选中状态更新到redis    user:userId:cart
         */
        Jedis jedis = redisUtil.getJedis();
        //定义 key
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        //使用hset获取 hget(key,field)  field 就是 skuId
        String CartJson = jedis.hget(userCartKey, skuId);
        //将字符串转换成对象
        CartInfo cartInfo = JSON.parseObject(CartJson, CartInfo.class);
        // 直接将选中状态赋给当前对象
        cartInfo.setIsChecked(isChecked);
        // 新的商品     转换成字符串
        String cartCheckdJson = JSON.toJSONString(cartInfo);
        // 将更新好的对象放入缓存  更新缓存
        jedis.hset(userCartKey,skuId,cartCheckdJson);

        // 将勾选中的商品，从新保存一份到  redis     user:2:checked -- 全部被选中的商品
        // 定义一个被选中商品的key
        String userCheckedKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CHECKED_KEY_SUFFIX;
        // 被选中的商品放入当前 userCheckedKey
        if ("1".equals(isChecked)){
            // 被选中添加
            jedis.hset(userCheckedKey,skuId,cartCheckdJson);
        }else {
            // 没有被选中则删除
            jedis.hdel(userCheckedKey,skuId);
        }
    }

    //根据 userId 查询选中的商品列表
    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        //获取redis中的key
        String userCheckedKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;
        Jedis jedis = redisUtil.getJedis();
        //用hval获取
        List<String> cartJsonList = jedis.hvals(userCheckedKey);
        List<CartInfo> cartInfoList = new ArrayList<>();
        if(cartJsonList!=null && cartJsonList.size()>0){
            //遍历
            for (String cartInfoJson : cartJsonList) {
                //将cartInfoJson 变成 cartInfo 对象
                CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
                //新建一个集合,把 cartInfo 对象放进去
                cartInfoList.add(cartInfo);
            }
        }
        return cartInfoList;
    }

    /**
     * 根据userId 查询数据库，获取购物车数据
     * @param userId
     * @return
     */
    private List<CartInfo> loadCartCache(String userId) {
        // 不能直接使用通用mapper。可能产生价格不统一的情况
        //自定义方法根据useId进行查询获取
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);
        // cartInfo --- skuInfo 中的价格不匹配。
        if (cartInfoList==null && cartInfoList.size()==0){
            //数据库中没有
            return null;
        }
        //数据库中有，并且成功获取到
        //准备 key ，准备放入缓存
        String userCartKey = CartConst.USER_KEY_PREFIX+userId+CartConst.USER_CART_KEY_SUFFIX;
        //创建 jedis 对象
        Jedis jedis = redisUtil.getJedis();
        // hset(key,field,value);
        Map<String,String> map = new HashMap<>(cartInfoList.size());
        // 添加从数据库查询出来的数据
        for (CartInfo cartInfo : cartInfoList) {
            // 将cartInfo转换成字符串
            String cartJson = JSON.toJSONString(cartInfo);
            map.put(cartInfo.getSkuId(),cartJson);
        }
        // map.put(key,value);
        jedis.hmset(userCartKey,map);
        return cartInfoList;
    }

}
