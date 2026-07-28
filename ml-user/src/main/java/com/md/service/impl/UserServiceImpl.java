package com.md.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.md.entity.User;
import com.md.mapper.UserMapper;
import com.md.service.UserService;
import org.springframework.stereotype.Service;

/**
 * 用户表 服务层实现。
 *
 * @author CM
 * @since v1.0.0
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>  implements UserService{

}
