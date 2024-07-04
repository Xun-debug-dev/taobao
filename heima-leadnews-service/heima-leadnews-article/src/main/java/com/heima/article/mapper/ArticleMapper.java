package com.heima.article.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface ArticleMapper extends BaseMapper<ApArticle> {
    /**
     *查询文章列表
     * @param dto
     * @param type =1表示加载更多， =2 表示加载更新
     * @return
     */
    List<ApArticle> loadArticleList(@Param("dto") ArticleHomeDto dto, @Param("type") Short type);

    /**
     * 获取当前时间前五天的文章列表
     * @param last5DayTime  比如现在时间是6/29，前五天的last5DayTime=6/24
     * @return
     */
    List<ApArticle> loadArticleListForLast5Days(@Param("last5DayTime") Date last5DayTime);

}
