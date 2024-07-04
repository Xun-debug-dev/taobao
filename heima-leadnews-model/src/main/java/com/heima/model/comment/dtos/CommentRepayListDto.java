package com.heima.model.comment.dtos;

import lombok.Data;

import java.util.Date;

/**
* 查询评论回复列表dto
*/
@Data
public class CommentRepayListDto {
/**
    * 评论id
*/
private String commentId;

private Date minDate;

private Integer size;
}
