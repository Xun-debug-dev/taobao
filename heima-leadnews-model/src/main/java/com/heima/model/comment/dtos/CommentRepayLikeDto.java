package com.heima.model.comment.dtos;

import lombok.Data;

/**
* 评论回复点赞
*/
@Data
public class CommentRepayLikeDto {

/**
   * 评论回复id
*/
private String commentRepayId;

/**
   * 0：点赞
   * 1：取消点赞
*/
private Short operation;
}
