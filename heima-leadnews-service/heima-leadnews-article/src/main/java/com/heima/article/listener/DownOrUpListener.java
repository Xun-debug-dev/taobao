package com.heima.article.listener;

import com.alibaba.fastjson.JSON;
import com.heima.article.service.ApArticleConfigService;
import com.heima.model.article.pojos.ApArticleConfig;
import com.heima.model.common.constants.WmNewsMessageConstants;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DownOrUpListener {

    @Autowired
    private ApArticleConfigService apArticleConfigService;
    /*
    监听文章上下架功能
     */
   @RabbitListener(
           bindings = @QueueBinding(
                   value = @Queue(name = WmNewsMessageConstants.WM_NEWS_UP_OR_DOWN_QUEUE,durable = "true"),
                   exchange = @Exchange(name = WmNewsMessageConstants.WM_NEWS_UP_OR_DOWN_TOPIC,type = ExchangeTypes.TOPIC),
                   key = WmNewsMessageConstants.WM_NEWS_UP_OR_DOWN_ROUTINGKEY
           )
   )
    public void getMessage(String  message){
       //1.将消息转为MAP类型
       Map map = JSON.parseObject(message, Map.class);

       //2.获取app文章ID和wemedia文章的状态
       Long articleId = Long.valueOf(String.valueOf(map.get("articleId")));
       Short enable = Short.valueOf(String.valueOf(map.get("enable")));  // 0-未上架  1-未下架

       //3.更新app端文章配置表的is_down的值   update ap_article_config set is_down=? where article_id=?
       apArticleConfigService.lambdaUpdate()
               .set(ApArticleConfig::getIsDown, enable == 0)
               .eq(ApArticleConfig::getArticleId, articleId)
               .update()
       ;
    }
}
