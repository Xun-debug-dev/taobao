package com.heima.search.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.heima.model.common.constants.ArticleConstants;
import com.heima.model.search.dtos.SearchArticleVo;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class SyncArticleListener {

    @Autowired
    private RestHighLevelClient client;


    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(name = ArticleConstants.ARTICLE_ES_SYNC_QUEUE,durable = "true"),
                    exchange = @Exchange(name = ArticleConstants.ARTICLE_ES_SYNC_TOPIC,type = ExchangeTypes.TOPIC),
                    key = ArticleConstants.ARTICLE_ES_SYNC_ROUTINGKEY
            )
    )
    public void getMessage(String message){
        //1.将JSON转为SearchArticle实例
        SearchArticleVo articleVo = JSON.parseObject(message, SearchArticleVo.class);

        //2.创建添加请求对象
        IndexRequest indexRequest=new IndexRequest("app_info_article").id(articleVo.getId()+"");

        //3.为suggestion添加数据，包含title和content
        List<String> suggestion=fengzhuangSuggestion(articleVo);
        articleVo.setSuggestion(suggestion);

        //4.指定添加文档的来源数据
        String data= JSON.toJSONString(articleVo);

        //5.添加文档数据
        indexRequest.source(data, XContentType.JSON);

        //6.执行导入
        try {
            IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
            System.out.println(indexResponse.getResult().getLowercase());
        } catch (IOException e) {
            e.printStackTrace();
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
