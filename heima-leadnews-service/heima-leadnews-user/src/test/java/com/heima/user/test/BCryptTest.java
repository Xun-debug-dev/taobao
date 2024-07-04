package com.heima.user.test;

import com.heima.utils.common.BCrypt;
import org.junit.Test;

public class BCryptTest {


    @Test
    public void testEncrypt(){
        for (int i = 0; i < 10; i++) {
            String password = "123456"; //用户的明文密码
            String salt = BCrypt.gensalt();//随机字符串（具有不可预测性）
            String passwordEncrypt = BCrypt.hashpw(password, salt);
            System.out.println(salt + " ====> " + passwordEncrypt);
        }
    }


    @Test
    public void testVerifyPassword(){
        String pwdLogin = "123456"; //登录的明文密码
        String pwdDB = "$2a$10$.EGQ8.G78CGJHQx7Fx0V.O3onFwwdItasJhfe6eXLV0K6kU4Qvv1a"; //表中的密文密码
        boolean result = BCrypt.checkpw(pwdLogin, pwdDB);
        if(result){
            System.out.println("密码正确");
        } else {
            System.out.println("密码错误");
        }
    }
}
