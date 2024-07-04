package com.heima.wemedia.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmLoginDto;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.common.AppJwtUtil;
import com.heima.utils.common.BCrypt;
import com.heima.wemedia.mapper.WmUserMapper;
import com.heima.wemedia.service.WmUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class WmUserServiceImpl extends ServiceImpl<WmUserMapper, WmUser> implements WmUserService {

    @Override
    public ResponseResult login(WmLoginDto dto) {
        //1.判断参数是否为空
        if(StringUtils.isBlank(dto.getName()) || StringUtils.isBlank(dto.getPassword())){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE);
        }

        //2.判断用户是否存在
        WmUser wmUser = this.lambdaQuery().eq(WmUser::getName, dto.getName()).one();
        if(wmUser==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "用户不存在");
        }

        //3.判断密码是否正确
        boolean flag = BCrypt.checkpw(dto.getPassword(), wmUser.getPassword());
        if(!flag){
            return ResponseResult.errorResult(AppHttpCodeEnum.LOGIN_PASSWORD_ERROR);
        }

        //4.判断状态是否正确
        if(wmUser.getStatus()!=9){
            return ResponseResult.errorResult(AppHttpCodeEnum.SERVER_ERROR, "状态不对");
        }

        //5.为用户生成Token
        Map map = new HashMap();

        wmUser.setPassword("");
        String token = AppJwtUtil.getToken(wmUser.getId().longValue());
        map.put("token", token);
        map.put("user",wmUser);

        return ResponseResult.okResult(map);
    }
}