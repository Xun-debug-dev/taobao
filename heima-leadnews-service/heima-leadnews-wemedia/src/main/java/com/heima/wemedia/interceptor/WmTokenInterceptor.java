package com.heima.wemedia.interceptor;

import com.heima.common.exception.CustomException;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.utils.threadlocal.WmThreadLocalUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 自定义的拦截器
 */
public class WmTokenInterceptor implements HandlerInterceptor {
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
        //1.获取请求头中的userId
        String userId = request.getHeader("userId");

        //2.如果userId是大于0则设置到ThreadLocal中
        if (StringUtils.isNotBlank(userId) && Integer.valueOf(userId) > 0) {
            WmThreadLocalUtil.setUser(Integer.valueOf(userId));
            return true; //放行
        }

        //3.拦截
        throw new CustomException(AppHttpCodeEnum.NEED_LOGIN);
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
        WmThreadLocalUtil.clear();
    }
}
