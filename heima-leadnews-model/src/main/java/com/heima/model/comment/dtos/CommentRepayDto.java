package com.heima.model.comment.dtos;

import lombok.Data;

/**
* 评论回复发送dto
*/
@Data
public class CommentRepayDto {

/**
   * 评论id
*/
private String commentId;

/**
   * 评论内容
*/
private String content;
}