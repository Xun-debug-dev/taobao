package com.heima.comment.controller;

import com.heima.comment.service.CommentRepayService;
import com.heima.model.comment.dtos.CommentRepayDto;
import com.heima.model.comment.dtos.CommentRepayLikeDto;
import com.heima.model.comment.dtos.CommentRepayListDto;
import com.heima.model.common.dtos.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/comment_repay")
public class CommentRepayController {

    @Autowired
    private CommentRepayService commentRepayService;
    /**
     * 发表评论回复--针对评论
     * @param dto
     * @return
     */
    @PostMapping("/save")
    public ResponseResult saveComment(@RequestBody CommentRepayDto dto){
        return commentRepayService.saveComment(dto);
    }

    /**
     * 点赞评论回复动作
     * @param dto
     * @return
     */
    @PostMapping("/like")
    public ResponseResult likeComment(@RequestBody CommentRepayLikeDto dto){
        return commentRepayService.likeComment(dto);
    }

    /**
     * 查询评论回复列表
     * @param dto
     * @return
     */
    @PostMapping("/load")
    public ResponseResult loadComment(@RequestBody CommentRepayListDto dto){
        return commentRepayService.loadComment(dto);
    }
}
