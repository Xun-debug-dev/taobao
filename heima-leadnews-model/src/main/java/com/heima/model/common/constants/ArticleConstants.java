package com.heima.model.common.constants;

public class ArticleConstants {
    //加载更多
    public static final Short LOADTYPE_LOAD_MORE = 1;
    //加载更新
    public static final Short LOADTYPE_LOAD_NEW = 2;
    //默认推荐频道id
    public static final String DEFAULT_TAG = "__all__";
    //文章同步es的交换机
    public static final String ARTICLE_ES_SYNC_TOPIC = "article.es.sync.topic";
    //文章同步es的队列
    public static final String ARTICLE_ES_SYNC_QUEUE="article.es.sync.queue";
    //文章同步es的路由key
    public static final String ARTICLE_ES_SYNC_ROUTINGKEY="article.es.sync.routingkey";


    public static final Integer HOT_ARTICLE_LIKE_WEIGHT = 3;
    public static final Integer HOT_ARTICLE_COMMENT_WEIGHT = 5;
    public static final Integer HOT_ARTICLE_COLLECTION_WEIGHT = 8;

    //存入到redis中key的前缀
    public static final String HOT_ARTICLE_FIRST_PAGE = "hot_article_first_page_";

}