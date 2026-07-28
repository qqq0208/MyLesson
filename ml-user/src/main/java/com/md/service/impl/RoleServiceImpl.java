package com.md.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.md.entity.Role;
import com.md.mapper.RoleMapper;
import com.md.service.RoleService;
import org.springframework.stereotype.Service;

/**
 * 角色表 服务层实现。
 *
 * @author CM
 * @since v1.0.0
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role>  implements RoleService{

}
