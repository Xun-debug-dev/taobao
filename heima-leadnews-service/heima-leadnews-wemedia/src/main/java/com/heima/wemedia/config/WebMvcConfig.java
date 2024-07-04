package com.heima.wemedia.config;

import com.heima.wemedia.interceptor.WmTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new WmTokenInterceptor())
                .addPathPatterns("/**")
                //放行swagger
                .excludePathPatterns("/swagger-ui.html/**","/swagger-resources/**","/v2/**","/webjars/**")
                .excludePathPatterns("/login/in")
                .excludePathPatterns("/api/v1/channel/channels");
    }
}
