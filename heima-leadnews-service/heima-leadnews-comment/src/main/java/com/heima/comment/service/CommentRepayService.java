package com.heima.comment.service;

import com.heima.model.comment.dtos.CommentRepayDto;
import com.heima.model.comment.dtos.CommentRepayLikeDto;
import com.heima.model.comment.dtos.CommentRepayListDto;
import com.heima.model.common.dtos.ResponseResult;

public interface CommentRepayService {
    /**
     * 发表评论回复--针对评论
     * @param dto
     * @return
     */
    ResponseResult saveComment(CommentRepayDto dto);
    /**
     * 点赞评论回复动作
     * @param dto
     * @return
     */
    ResponseResult likeComment(CommentRepayLikeDto dto);
    /**
     * 查询评论回复列表
     * @param dto
     * @return
     */
    ResponseResult loadComment(CommentRepayListDto dto);
}
