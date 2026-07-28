package com.md.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.md.entity.UserRole;
import com.md.mapper.UserRoleMapper;
import com.md.service.UserRoleService;
import org.springframework.stereotype.Service;

/**
 * 用户角色关系表 服务层实现。
 *
 * @author CM
 * @since v1.0.0
 */
@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole>  implements UserRoleService{

}
