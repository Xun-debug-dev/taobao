package com.heima.article.config;

import com.heima.model.common.constants.ArticleConstants;
import com.heima.model.common.constants.WmNewsMessageConstants;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    /**
     * 同步索引库交换机定义
     * @return
     */
    @Bean
    public Exchange syncEsExchange(){
        return ExchangeBuilder.topicExchange(ArticleConstants.ARTICLE_ES_SYNC_TOPIC).durable(true).build();
    }

    /**
     *  同步索引库队列定义
     * @return
     */
    @Bean
    public Queue syncEsQueue(){
        return QueueBuilder.durable(ArticleConstants.ARTICLE_ES_SYNC_QUEUE).build();
    }

    /**
     *  同步索引库交换机和队列绑定
     * @return
     */
    @Bean
    public Binding syncEsBinding(){
        return BindingBuilder.bind(syncEsQueue()).to(syncEsExchange()).with(ArticleConstants.ARTICLE_ES_SYNC_ROUTINGKEY).noargs();
    }
}
