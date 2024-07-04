package com.heima.api.feign;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.user.pojo.ApUser;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("leadnews-user")
public interface ApUserFeignClient {

    /**
     * 根据用户id获取用户实体
     * @param userId
     * @return
     */
    @GetMapping("/api/v1/user/{userId}")
    ResponseResult<ApUser>findUserById(@PathVariable Integer userId);
}
