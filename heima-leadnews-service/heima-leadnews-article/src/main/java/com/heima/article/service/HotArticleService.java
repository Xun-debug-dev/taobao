package com.heima.article.service;

import com.heima.model.message.ArticleVisitStreamMess;

public interface HotArticleService {
    /**
     * 定时任务计算热点文章，没有返回值，也没有参数
     */
    void computeHotArticleToRedis();

    /**
     * 实时计算，更新文章分值的方法
     * @param mess
     */
    void updateScore(ArticleVisitStreamMess mess);
}
