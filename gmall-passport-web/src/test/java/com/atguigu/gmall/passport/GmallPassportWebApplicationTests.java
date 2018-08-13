package com.atguigu.gmall.passport;

import com.atguigu.gmall.passport.util.JwtUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallPassportWebApplicationTests {

	@Test
	public void contextLoads() {
	}

	@Test
	public void test01(){
		//共有部分
		String key = "atguigu";
		//盐
		String ip="192.168.154.131";
		//私有部分
		Map map = new HashMap();
		map.put("userId","1001");
		map.put("nickName","marry");


		String token = JwtUtil.encode(key, map, ip);
		System.out.println("token=" + token);
		//解密
		Map<String, Object> objectMap = JwtUtil.decode(token, key, ip);

		//为了防止解密方式不正确，objectMap为空，报空指针异常，所以先加个判断
		if(objectMap!=null){
			System.out.println("userId=" + objectMap.get("userId"));
		}
	}
}
