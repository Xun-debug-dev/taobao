package com.heima.behavior.controller.v1;

import com.heima.behavior.service.BehaviorService;
import com.heima.model.behavior.dtos.ArticleBehaviorDto;
import com.heima.model.common.dtos.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/behavior")
public class ApBehaviorController {

    @Autowired
    private BehaviorService behaviorServcie;

    /**
     * 加载行为列表
     *
     * @return
     */
    @PostMapping(path = "/load_article_behavior")
    public ResponseResult loadArticleBehavior(@RequestBody ArticleBehaviorDto dto) {
        return behaviorServcie.loadArticleBehavior(dto);
    }
}