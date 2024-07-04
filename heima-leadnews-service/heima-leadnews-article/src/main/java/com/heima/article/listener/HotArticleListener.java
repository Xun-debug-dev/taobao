package com.heima.article.listener;

import com.alibaba.fastjson.JSONObject;
import com.heima.article.service.HotArticleService;
import com.heima.model.message.ArticleVisitStreamMess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class HotArticleListener {
    @Autowired
    private HotArticleService hotArticleService;

    //@KafkaListener(topics = HotArticleConstants.HOT_ARTICLE_INCR_HANDLE_TOPIC)
    public void getMessage(String message){
        //1.解析对象
        ArticleVisitStreamMess mess = JSONObject.parseObject(message, ArticleVisitStreamMess.class);
        //2.调用service接口
        hotArticleService.updateScore(mess);
    }
}
