package com.md.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.md.entity.RoleMenu;
import com.md.mapper.RoleMenuMapper;
import com.md.service.RoleMenuService;
import org.springframework.stereotype.Service;

/**
 * 角色菜单关系表 服务层实现。
 *
 * @author CM
 * @since v1.0.0
 */
@Service
public class RoleMenuServiceImpl extends ServiceImpl<RoleMenuMapper, RoleMenu>  implements RoleMenuService{

}
