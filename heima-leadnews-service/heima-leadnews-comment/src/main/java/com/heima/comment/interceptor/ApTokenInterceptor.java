package com.heima.comment.interceptor;

import com.heima.model.user.pojo.ApUser;
import com.heima.utils.common.AppJwtUtil;
import com.heima.utils.threadlocal.AppThreadLocalUtil;
import io.jsonwebtoken.Claims;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 自定义的拦截器
 */
public class ApTokenInterceptor implements HandlerInterceptor {
    /**
     * 执行目标方法之前执行的过滤器方法
     * @param request
     * @param response
     * @param handler
     * @return  true表示放行，false表示拦截
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //1.从请求头对象中获取token
        String token = request.getHeader("token");
        //2.解析token中的id值
        Claims claimsBody = AppJwtUtil.getClaimsBody(token);
        Object id = claimsBody.get("id");

        //3.把id存入到ThreadLocal对象中
        ApUser apUser=new ApUser();
        apUser.setId((Integer) id);
        AppThreadLocalUtil.setUser(apUser);
        //4.放行
        return true;
    }

    /**
     * 完成所有的请求之后，执行的方法
     * @param request
     * @param response
     * @param handler
     * @param ex
     * @throws Exception
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //释放资源
        AppThreadLocalUtil.clear();
    }
}
