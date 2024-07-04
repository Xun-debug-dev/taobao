package com.heima.comment.pojos;

import lombok.Data;

@Data
public class ApCommentVo extends ApComment {

 /**
     * 0：点赞
     * 1：取消点赞
  */
 private Short operation;
}