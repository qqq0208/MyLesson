package com.md.service;

import com.md.dto.MenuInsertDTO;
import com.md.dto.MenuPageDTO;
import com.md.dto.MenuUpdateDTO;
import com.md.vo.MenuSimpleListVO;
import com.md.vo.PageVO;
import com.mybatisflex.core.service.IService;
import com.md.entity.Menu;

import java.util.List;

/**
 * 菜单表 服务层。
 *
 * @author CM
 * @since v1.0.0
 */
public interface MenuService extends IService<Menu> {

    //添加 - 单条
    boolean insert(MenuInsertDTO dto);

    //查询 - 单条
    Menu select(Long id);

    //查询 - 简单列表
    List<MenuSimpleListVO> simpleList();

    //查询 - 条件分页
    PageVO<Menu> page(MenuPageDTO dto);

    //修改 - 单条
    boolean update(MenuUpdateDTO dto);

    //删除 - 单条
    boolean delete(Long id);

    //删除 - 批量
    boolean deleteBatch(List<Long> ids);

    //查询 - 角色菜单的ID
    List<Long> listMenuIdsByRoleId(Long roleId);

    //修改 - 角色菜单
    boolean updateMenusByRoleId(Long roleId, List<Long> menuIds);


}
