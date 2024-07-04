package com.heima.model.behavior.dtos;

import com.heima.model.common.annotation.IdEncrypt;
import lombok.Data;

@Data
public class FollowBehaviorDto {
    /**
         * 文章id
    */
    @IdEncrypt
    private Long articleId;

    /**
         * 关注的id
    */
    private Integer authorId;

    /**
         * 操作方式
         * 0  关注
         * 1  取消
    */
    private short operation;
}