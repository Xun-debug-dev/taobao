package com.heima.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.user.dtos.LoginDto;
import com.heima.model.user.pojo.ApUser;
import org.springframework.web.bind.annotation.RequestBody;

public interface ApUserService extends IService<ApUser> {

    /**
     * 登录接口
     * @param dto
     * @return
     */
    ResponseResult login(@RequestBody LoginDto dto);

    /**
     * 根据用户id获取用户实体
     * @param userId
     * @return
     */
    ResponseResult<ApUser>findUserById(Integer userId);
}
