package com.heima.model.search.dtos;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class SearchArticleVo {

    // 文章id
    private Long id;
    // 文章标题
    private String title;
    // 文章发布时间
    private Date publishTime;
    // 文章布局
    private Integer layout;
    // 封面
    private String images;
    // 作者id
    private Long authorId;
    // 作者名词
    private String authorName;
    //静态url
    private String staticUrl;
    //文章内容
    private String content;

    //定义一个自动补全的属性
    private List<String> suggestion;

}