package com.heima.behavior.controller.v1;

import com.heima.behavior.service.BehaviorService;
import com.heima.model.behavior.dtos.FollowBehaviorDto;
import com.heima.model.common.dtos.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/follow_behavior")
public class BehaviorController {

    @Autowired
    private BehaviorService behaviorService;

    /**
     * 用户关注行为
     * @param dto
     * @return
     */
    @PostMapping("/user_follow")
    public ResponseResult userFollow(@RequestBody FollowBehaviorDto dto){
        return behaviorService.userFollow(dto);
    }
}
