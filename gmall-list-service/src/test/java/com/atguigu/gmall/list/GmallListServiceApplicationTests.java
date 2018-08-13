package com.atguigu.gmall.list;

import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;
import com.atguigu.gmall.service.ListService;
import io.searchbox.client.JestClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallListServiceApplicationTests {

	@Test
	public void contextLoads() {
	}
	@Autowired
	JestClient jestClient;

	@Autowired
	private ListService listService;

/*	@Test
	public void testEs() throws IOException {
		String query=
				"{\n" +
						"  \"query\": {\n" +
						"    \"match\": {\n" +
						"      \"actorList.name\": \"张译\"\n" +
						"    }\n" +
						"  }\n" +
						"}";
		Search search = new Search.Builder(query).addIndex("movie_chn").addType("movie").build();

		SearchResult result = jestClient.execute(search);

		List<SearchResult.Hit<HashMap, Void>> hits = result.getHits(HashMap.class);

		for (SearchResult.Hit<HashMap, Void> hit : hits) {
			HashMap source = hit.source;
			System.err.println("source = " + source);
		}

	}*/

	@Test
	public void  testEsDsl(){
		SkuLsParams skuLsParams = new SkuLsParams();
		skuLsParams.setKeyword("小米");
		skuLsParams.setCatalog3Id("61");
		skuLsParams.setPageNo(1);
		skuLsParams.setPageSize(5);
		skuLsParams.setValueId(new String[]{"83"});
		SkuLsResult search = listService.search(skuLsParams);
		System.out.println(search.toString());
	}


}
