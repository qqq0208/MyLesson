package com.md.fallback;

import com.md.entity.User;
import com.md.feign.UserFeign;
import com.md.result.Result;
import org.springframework.stereotype.Component;

/**
 * @author CM
 **/
@Component
public class UserFeignFallBack implements UserFeign {

    @Override
    public Result<User> select(Long id) {
        System.err.println("用户微服务远程调用失败 请联系管理员");
        return null;
    }
}
