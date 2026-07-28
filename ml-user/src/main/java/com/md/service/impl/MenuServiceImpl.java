package com.md.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.md.entity.Menu;
import com.md.mapper.MenuMapper;
import com.md.service.MenuService;
import org.springframework.stereotype.Service;

/**
 * 菜单表 服务层实现。
 *
 * @author CM
 * @since v1.0.0
 */
@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu>  implements MenuService{

}
