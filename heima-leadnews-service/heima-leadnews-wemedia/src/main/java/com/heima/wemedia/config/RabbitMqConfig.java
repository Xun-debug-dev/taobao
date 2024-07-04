package com.heima.wemedia.config;

import com.heima.model.common.constants.WmNewsMessageConstants;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
    /**
     * 上下架交换机定义
     * @return
     */
    @Bean
    public Exchange downOrUpExchange(){
        return ExchangeBuilder.topicExchange(WmNewsMessageConstants.WM_NEWS_UP_OR_DOWN_TOPIC).durable(true).build();
    }

    /**
     * 上下架队列定义
     * @return
     */
    @Bean
    public Queue downOrUpQueue(){
        return QueueBuilder.durable(WmNewsMessageConstants.WM_NEWS_UP_OR_DOWN_QUEUE).build();
    }

    /**
     * 上下架交换机和队列绑定
     * @return
     */
    @Bean
    public Binding downOrUpBinding(){
        return BindingBuilder.bind(downOrUpQueue()).to(downOrUpExchange()).with(WmNewsMessageConstants.WM_NEWS_UP_OR_DOWN_ROUTINGKEY).noargs();
    }
}
