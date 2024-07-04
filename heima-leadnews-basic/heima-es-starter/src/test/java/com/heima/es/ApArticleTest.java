package com.heima.es;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.heima.es.mapper.ApArticleMapper;
import com.heima.es.pojo.SearchArticleVo;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@SpringBootTest
@RunWith(SpringRunner.class)
public class ApArticleTest {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private ApArticleMapper apArticleMapper;

    /**
     * 注意：数据量的导入，如果数据量过大，需要分页导入
     * @throws Exception
     */
    @Test
    public void init() throws Exception {
        //1.查询mysql所有数据
        List<SearchArticleVo> searchArticleVos = apArticleMapper.loadArticleList();


        //3.创建批量请求对象
        BulkRequest bulkRequest=new BulkRequest();
        for (SearchArticleVo articleVo : searchArticleVos) {
            //5.创建添加请求对象
            IndexRequest indexRequest=new IndexRequest("app_info_article").id(articleVo.getId()+"");

            //为suggestion添加数据，包含title和content
            List<String> suggestion=fengzhuangSuggestion(articleVo);
            articleVo.setSuggestion(suggestion);


            //7.指定添加文档的来源数据
            String data= JSON.toJSONString(articleVo);
            //6.添加文档数据
            indexRequest.source(data, XContentType.JSON);

            //4.批量添加
            bulkRequest.add(indexRequest);
        }

        //2.执行批量导入
        BulkResponse bulkResponse = restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        BulkItemResponse[] items = bulkResponse.getItems();
        for (BulkItemResponse item : items) {
            String result = item.getResponse().getResult().getLowercase();//created
            System.out.println(result);
        }


    }

    /**
     * 解决封装suggestion的方法
     * @param articleVo
     */
    private List<String > fengzhuangSuggestion(SearchArticleVo articleVo) {

        //1.实例化list
        List<String > suggesion=new ArrayList<>();

        //2.解析content
        String content = articleVo.getContent();
        List<Map> contentList = JSONArray.parseArray(content, Map.class);
        for (Map map : contentList) {
            if(map.get("type").equals("text")){
                String value = (String) map.get("value");
                String[] split = value.split(",");
                suggesion.addAll(Arrays.asList(split));
            }
        }
        //添加标题
        suggesion.add(articleVo.getTitle());

        return suggesion;
    }


}