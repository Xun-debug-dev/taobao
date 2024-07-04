package com.heima.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.dtos.LoginDto;
import com.heima.model.user.pojo.ApUser;
import com.heima.user.mapper.ApUserMapper;
import com.heima.user.service.ApUserService;
import com.heima.utils.common.AppJwtUtil;
import com.heima.utils.common.BCrypt;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ApUserServiceImpl extends ServiceImpl<ApUserMapper, ApUser> implements ApUserService {

    /**
     * 登录接口
     *   二合一，用户登录也是有游客登录
     * @param dto
     * @return
     */
    @Override
    public ResponseResult login(LoginDto dto) {

        Map result = new HashMap();

        //1. 正常用户登录处理
        if(StringUtils.isNotBlank(dto.getPhone()) && StringUtils.isNotBlank(dto.getPassword())){

            //1.1 判断用户是否存在   select * from ap_user where phone=?
//            LambdaQueryWrapper<ApUser> lambdaQueryWrapper = new LambdaQueryWrapper<>();
//            lambdaQueryWrapper.eq(ApUser::getPhone,dto.getPhone());
//            ApUser apUser = this.getOne(lambdaQueryWrapper);

//            ApUser apUser = this.getOne(Wrappers.<ApUser>lambdaQuery().eq(ApUser::getPhone, dto.getPhone()));

            ApUser apUser = this.lambdaQuery().eq(ApUser::getPhone, dto.getPhone()).one();

            if(apUser==null){
                return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "用户不存在");
            }

            //1.2 判断用户密码是否正确
            String loginPwd = dto.getPassword(); //登录的明文密码
            String dbPwd = apUser.getPassword();//表中的密文密码
            boolean flag = BCrypt.checkpw(loginPwd, dbPwd);
            if(!flag){
                return ResponseResult.errorResult(AppHttpCodeEnum.LOGIN_PASSWORD_ERROR);
            }

            //1.3 判断用户状态是否正确
            if(apUser.getStatus()){
                return ResponseResult.errorResult(AppHttpCodeEnum.STATUS_INVALID);
            }

            //1.4 生成用户Token
            String token = AppJwtUtil.getToken(apUser.getId().longValue()); //使用用户的ID生成Token
            result.put("token",token);

            apUser.setPassword("");
            result.put("user",apUser);

        } else {
            //2. 游客登录处理
            String token = AppJwtUtil.getToken(0L); //统一的为所有游客使用0生成Token
            result.put("token",token);
        }
        return ResponseResult.okResult(result);
    }

    /**
     * 根据用户id获取用户实体
     *
     * @param userId
     * @return
     */
    @Override
    public ResponseResult<ApUser>findUserById(Integer userId) {
        ApUser apUser = getById(userId);
        return ResponseResult.okResult(apUser);
    }
}
