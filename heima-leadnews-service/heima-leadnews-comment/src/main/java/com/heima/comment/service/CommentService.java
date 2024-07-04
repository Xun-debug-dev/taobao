package com.heima.comment.service;

import com.heima.model.comment.dtos.CommentLikeDto;
import com.heima.model.comment.dtos.CommentListDto;
import com.heima.model.comment.dtos.CommentSaveDto;
import com.heima.model.common.dtos.ResponseResult;

public interface CommentService {

    /**
     * 发表评论--针对文章
     * @param dto
     * @return
     */
    ResponseResult saveComment(CommentSaveDto dto);


    /**
     * 点赞评论动作
     * @param dto
     * @return
     */
    ResponseResult likeComment(CommentLikeDto dto);


    /**
     * 查询文章的评论列表
     * @param dto
     * @return
     */
    ResponseResult loadComment(CommentListDto dto);
}
