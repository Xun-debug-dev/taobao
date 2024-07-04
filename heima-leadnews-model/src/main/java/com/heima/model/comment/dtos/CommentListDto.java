package com.heima.model.comment.dtos;

import lombok.Data;

import java.util.Date;

@Data
public class CommentListDto {

    /**
        * 文章id
     */
    private Long articleId;

    /**
        * 显示条数
     */
    private Integer size;

    /**
        * 最小时间
     */
    private Date minDate;
}