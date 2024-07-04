package com.heima.gateway.filter;

import com.heima.gateway.utils.AppJwtUtil;
import io.jsonwebtoken.Claims;
import org.apache.commons.lang.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/*
全局过滤器，校验token是否有效
 */
@Component
public class AuthorizeFilter implements GlobalFilter, Ordered {
    /**
     * 执行过滤的方法
     * @param exchange 交换机，可以获取请求和响应对象
     * @param chain 过滤器链
     * @return
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //1. 获取请求实例和响应实例
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        //2. 获取请求路径
        String path = request.getURI().getPath();

        //3. 如果是登录则放行请求
        if(path.equals("/user/api/v1/login/login_auth/")){
            return chain.filter(exchange);
        }

        //4. 获取请求头里的Token
        String token = request.getHeaders().getFirst("token");

        //5. 如果Token无值则响应401
        if(StringUtils.isBlank(token)){
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        //6. 如果Token非法则响应401
        Claims claimsBody = AppJwtUtil.getClaimsBody(token);
        int result = AppJwtUtil.verifyToken(claimsBody);
        if(result==1 || result==2){
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        //7. Token合法则放行请求
        return chain.filter(exchange);
    }

    /**
     * 过滤的执行的优先级，返回值越小优先级越高
     */
    @Override
    public int getOrder() {
        return 0;
    }
}
