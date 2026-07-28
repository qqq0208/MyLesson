package com.md.filter;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.JschUtil;
import cn.hutool.json.JSONUtil;
import io.netty.handler.codec.http.FullHttpMessage;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @Author: CM
 * @Description TODO
 */
@Component
public class TokenGlobalFilter implements GlobalFilter, Ordered {

    //读取配置文件中指定内容
    @Value("${token.white_list}")
    private List<String> whiteList;

    //操作Redis对象
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //打印白名单数据 测试是否读取到了远程的文件内容
        for (String s : whiteList) {
            System.out.println(s);
        }
        //读取请求对象和响应对象
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        //如果当前请求的地址在白名单 直接放行
        if (isWhile(request)) {
            return chain.filter(exchange);
        }
        //获取token
        String token = getToken(request);
        if (StrUtil.isBlank(token)) {
            //响应结果--->登录过期
            return buildResponseData(response, 6000, "登录过期", "请求中未携带Token令牌");
        }
        //从Redis中获取token
        String tokenMessages = redisTemplate.opsForValue().get(token);

        if (StrUtil.isBlank(tokenMessages)) {
            //响应结果--->登录过期
            return buildResponseData(response, 6000, "登录过期", "Redis不存在该Token令牌");
        }
        //将Redis中的token进行续期 重新设置过期时间
        redisTemplate.expire(token, 30, TimeUnit.MINUTES);
        //放行请求
        return chain.filter(exchange);
    }

    //优先级最高
    @Override
    public int getOrder() {
        return 0;
    }


    private boolean isWhile(ServerHttpRequest request) {
        //获取当前请求地址
        String url = request.getURI().toString();
        boolean result = false;
        for (String white : whiteList) {
            if (url.contains(white)) {
                result = true;
                break;
            }

        }
        return result;

    }

    //尝试从请求头或者请求参数中获取token令牌
    private String getToken(ServerHttpRequest request) {
        //尝试从请求头中获取
        String token = request.getHeaders().getFirst("token");
        if (StrUtil.isBlank(token)) {
            //尝试直接从查询参数中获取
            token = request.getQueryParams().getFirst("token");
        }
        return token;
    }

    //构造响应数据
    private Mono<Void> buildResponseData(ServerHttpResponse response, int code, String message, String codeMessage) {
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        //设置响应数据
        Map<String, Object> resultMap = Map.of("code", code, "message", message, "codeMessage", codeMessage);
        String resultStr = JSONUtil.toJsonStr(resultMap);
        //响应
        byte[] bytes = resultStr.getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Flux.just(response.bufferFactory().wrap(bytes)));
    }

}