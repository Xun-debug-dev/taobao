package com.heima.behavior.service;

import com.heima.model.behavior.dtos.ArticleBehaviorDto;
import com.heima.model.behavior.dtos.FollowBehaviorDto;
import com.heima.model.behavior.dtos.LikesBehaviorDto;
import com.heima.model.behavior.dtos.ReadBehaviorDto;
import com.heima.model.common.dtos.ResponseResult;

public interface BehaviorService {

    /**
     * 用户关注行为
     * @param dto
     * @return
     */
    ResponseResult userFollow(FollowBehaviorDto dto);
    /**
     * 点赞行为
     * @param dto
     * @return
     */
    ResponseResult like(LikesBehaviorDto dto);

    /**
     * 阅读行为
     * @param dto
     * @return
     */
    ResponseResult readBehavior(ReadBehaviorDto dto);
    /**
     * 加载行为列表
     *
     * @return
     */
    ResponseResult loadArticleBehavior(ArticleBehaviorDto dto);
}
