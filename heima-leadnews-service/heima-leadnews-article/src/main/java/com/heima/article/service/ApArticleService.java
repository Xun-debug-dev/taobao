package com.heima.article.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.common.dtos.ResponseResult;


public interface ApArticleService extends IService<ApArticle> {

    /**
     * 加载首页-加载更多-加载更新 三位一体
     * @param dto
     *  type=1 认为是加载更多，type=2表示加载更新
     * @return
     */
    ResponseResult load(ArticleHomeDto dto, Short type);



    /**
     * 保存三剑客
     * @return
     */
    ResponseResult save(ArticleDto dto);

}
