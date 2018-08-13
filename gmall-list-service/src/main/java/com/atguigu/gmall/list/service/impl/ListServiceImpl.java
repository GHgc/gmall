package com.atguigu.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.*;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ListServiceImpl implements ListService {

    @Autowired
    JestClient jestClient;

    @Autowired
    RedisUtil redisUtil;

    public static final String ES_INDEX="gmall";
    public static final String ES_TYPE="SkuInfo";

    @Override
    public void saveSkuInfo(SkuLsInfo skuLsInfo) {
        //保存数据
        Index index = new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE).id(skuLsInfo.getId()).build();
        try {
            DocumentResult documentResult = jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public SkuLsResult search(SkuLsParams skuLsParams) {
        String query=makeQueryStringForSearch(skuLsParams);

        //准备根据query拼成的dsl语句进行查询
        Search search= new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();
        SearchResult searchResult=null;
        try {
            searchResult = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //返回对象应该是自定义封装好的对象
        SkuLsResult skuLsResult = makeResultForSearch(skuLsParams, searchResult);

        return skuLsResult;

    }

    //更新热度评分
    @Override
    public void incrHotScore(String skuId) {

        //redis
        Jedis jedis = redisUtil.getJedis();
        //      定义key
        String hotScore = "hotScore";
        String skuKey = "skuId:";


        //更新热度的规则
//       自定义步长，
        Double zincrby = jedis.zincrby(hotScore, 1, skuKey + skuId);
//      更新规则 ，每访问10次则更新一下es
        if (zincrby%10==0){
//            更新es中hotScore
            updateHotScore(  skuId,  Math.round(zincrby));
        }

    }

    private void updateHotScore(String skuId, long hotScore) {
        // es 的更新语句
        String updateJson="{\n" +
                "   \"doc\":{\n" +
                "     \"hotScore\":"+hotScore+"\n" +
                "   }\n" +
                "}";
        // 准备执行更新语句   new UpdateByQuery.Builder();
        Update update = new Update.Builder(updateJson).index(ES_INDEX).type(ES_TYPE).id(skuId).build();
        try {
            jestClient.execute(update);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //对结果集的转换
    private SkuLsResult makeResultForSearch(SkuLsParams skuLsParams, SearchResult searchResult) {
        SkuLsResult skuLsResult = new SkuLsResult();
        /**
         *   List<SkuLsInfo> skuLsInfoList;
         long total;
         long totalPages;
         List<String> attrValueIdList;
         */
        // 声明一个集合 List<SkuLsInfo> 存放 dsl 语句查询出来的数据
        // 规定了集合的大小，
        List<SkuLsInfo> skuLsInfoList=new ArrayList<>(skuLsParams.getPageSize());

        // 查找数据hits:
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
        for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
            // 取出普通skuInfo信息
            SkuLsInfo skuLsInfo = hit.source;
            // 将SkuLsInfo 中的skuName 属性赋值为高亮值，准备取得高亮 hit.highlight.size() 说明hit.highlight是一个集合
            if (hit.highlight!=null && hit.highlight.size()>0){
                List<String> list = hit.highlight.get("skuName");
                String skuNameES = list.get(0);
                skuLsInfo.setSkuName(skuNameES);
            }
            skuLsInfoList.add(skuLsInfo);
        }
        skuLsResult.setSkuLsInfoList(skuLsInfoList);
        // 记录总条数
        skuLsResult.setTotal(searchResult.getTotal());
        // 总页数
        // long pages = searchResult.getTotal()%skuLsParams.getPageSize()==0?searchResult.getTotal()/skuLsParams.getPageSize():(searchResult.getTotal()/skuLsParams.getPageSize()+1);
        long pages = (searchResult.getTotal() + skuLsParams.getPageSize()-1)/skuLsParams.getPageSize();
        skuLsResult.setTotalPages(pages);
        // 因为dsl 语句在聚合的时候，是根据skuAttrValueList.valueId 数据会存在聚合中
        MetricAggregation aggregations = searchResult.getAggregations();
        // 根据聚合名称，获取数据
        TermsAggregation groupby_attr = aggregations.getTermsAggregation("groupby_attr");
        List<TermsAggregation.Entry> buckets = groupby_attr.getBuckets();
        // 创建一个新的集合存储平台属性的Id
        List<String> attrValueIdList = new ArrayList<>();
        for (TermsAggregation.Entry bucket : buckets) {
            String valueId = bucket.getKey();
            attrValueIdList.add(valueId);
        }
        // 将平台属性值id放入返回数据模型对象
        skuLsResult.setAttrValueIdList(attrValueIdList);

        return skuLsResult;

    }

    //编写dsl语句
    private String makeQueryStringForSearch(SkuLsParams skuLsParams) {
        // 创建一个工具类
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        // 创建query，bool，filter。
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        // must,filter 属于同级。 must:skuName：小米 ,小米则相当于传递进来的关键字
        if (skuLsParams.getKeyword()!=null && skuLsParams.getKeyword().length()>0){
            // must 所对应的对象，MatchQueryBuilder skuName=小米
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName", skuLsParams.getKeyword());
            // must 属于bool 部分
            boolQueryBuilder.must(matchQueryBuilder);
            // skuName 除了有查询关键字之外，有高亮显示 highlight ，bool同一级
            HighlightBuilder highlighter = searchSourceBuilder.highlighter();
            highlighter.field("skuName");
            // 设置一下前缀，后缀 red
            highlighter.preTags("<span style='color:red'>");
            highlighter.postTags("</span>");
            // 设置高亮
            searchSourceBuilder.highlight(highlighter);
        }
//        设置三级分类Id
        if (skuLsParams.getCatalog3Id()!=null && skuLsParams.getCatalog3Id().length()>0){
            // 找到term对象
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id", skuLsParams.getCatalog3Id());
            // 添加三级分类id
            boolQueryBuilder.filter(termQueryBuilder);
        }
//        设置平台属性值Id
        if (skuLsParams.getValueId()!=null&& skuLsParams.getValueId().length>0){
//            遍历
            for (int i = 0; i < skuLsParams.getValueId().length; i++) {
                // 取处出平台属性值的Id
                String valueId = skuLsParams.getValueId()[i];
                // 将平台属性值Id 放入 term对象
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId",valueId);
                // 添加三级分类id
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }
//      设置分页
//      from 从第几条数据开始查询！ select * from skuInfo limit 0 ,5: 第一页有5条数据，条1开始， select * from skuInfo limit 5,5
        int from = skuLsParams.getPageSize()*((skuLsParams.getPageNo()-1));
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(skuLsParams.getPageSize());
//      排序
        searchSourceBuilder.sort("hotScore",SortOrder.DESC);
//      设置聚合aggs 的对象AggregationBuilders
        TermsBuilder groupby_attr = AggregationBuilders.terms("groupby_attr").field("skuAttrValueList.valueId");
//       将聚合对象添加到query中
        searchSourceBuilder.aggregation(groupby_attr);
        /*将bool 中的must*/
        searchSourceBuilder.query(boolQueryBuilder);

        // 利用查询工具调用query方法
        String query = searchSourceBuilder.toString();


        System.out.println("query:"+query);
        return query;

    }


}
