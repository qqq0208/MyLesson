package com.md.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.md.dto.MenuInsertDTO;
import com.md.dto.MenuPageDTO;
import com.md.dto.MenuUpdateDTO;
import com.md.entity.RoleMenu;
import com.md.exception.ServiceException;
import com.md.mapper.RoleMenuMapper;
import com.md.result.ResultCode;
import com.md.vo.MenuSimpleListVO;
import com.md.vo.PageVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.md.entity.Menu;
import com.md.mapper.MenuMapper;
import com.md.service.MenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.md.entity.table.MenuTableDef.MENU;
import static com.md.entity.table.RoleMenuTableDef.ROLE_MENU;

/**
 * 菜单表 服务层实现。
 *
 * @author CM
 * @since v1.0.0
 */
@Service
public class MenuServiceImpl extends ServiceImpl<MenuMapper, Menu> implements MenuService {

    @Autowired
    private RoleMenuMapper roleMenuMapper;

    @Override
    public boolean insert(MenuInsertDTO dto) {
        String title = dto.getTitle();
        //标题查重
        if (QueryChain.of(mapper)
                .where(MENU.TITLE.eq(title))
                .exists()) {
            throw new ServiceException(ResultCode.TITLE_REPEAT, "标题" + title + "重复");
        }
        // 组装实体类
        Menu menu = BeanUtil.copyProperties(dto, Menu.class);
        menu.setInfo(StrUtil.isEmpty(dto.getInfo()) ? "暂无描述。" : dto.getInfo());
        menu.setCreated(LocalDateTime.now());
        menu.setUpdated(LocalDateTime.now());
        //DB修改
        if (mapper.insert(menu) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据添加失败");
        }
        return true;
    }

    @Override
    public Menu select(Long id) {
        Menu menu = mapper.selectOneWithRelationsById(id);
        if (ObjectUtil.isNull(menu)) {
            throw new ServiceException(ResultCode.MENU_NOT_FOUND, id + "号数据不存在");
        }
        return menu;
    }

    @Override
    public List<MenuSimpleListVO> simpleList() {
        //关联查询菜单列表 如果当前菜单有父菜单 一并查出
        QueryWrapper wrapper = new QueryWrapper();
        wrapper.orderBy(MENU.PID.asc(), MENU.IDX.asc(), MENU.ID.desc());
        List<Menu> menus = mapper.selectListWithRelationsByQuery(wrapper);
        //将当前数据（Menu）转为 MenuSimpleListVO 数据集合
        List<MenuSimpleListVO> simpleListVOS = menus.stream().map(menu -> {
            MenuSimpleListVO menuSimpleListVO = BeanUtil.copyProperties(menu, MenuSimpleListVO.class);
            if (menu.getParentMenu() != null) {
                menuSimpleListVO.setParentTitle(menu.getParentMenu().getTitle());
            }
            return menuSimpleListVO;
        }).collect(Collectors.toList());
        return simpleListVOS;
    }

    @Override
    public PageVO<Menu> page(MenuPageDTO dto) {
        QueryChain<Menu> queryChain = QueryChain.of(mapper)
                .orderBy(MENU.PID.asc(), MENU.IDX.asc(), MENU.ID.desc());

        // pid条件
        if (ObjectUtil.isNotNull(dto.getPid())) {
            queryChain.where(MENU.PID.eq(dto.getPid()));
        }

        // title条件
        if (ObjectUtil.isNotNull(dto.getTitle())) {
            queryChain.where(MENU.TITLE.like(dto.getTitle()));
        }

        // DB分页并转为VO
        Page<Menu> result = queryChain.withRelations().page(new Page<>(dto.getPageNum(), dto.getPageSize()));
        PageVO<Menu> pageVO = new PageVO<>();
        BeanUtil.copyProperties(result, pageVO);
        pageVO.setPageNum(result.getPageNumber());
        return pageVO;
    }

    @Override
    public boolean update(MenuUpdateDTO dto) {
        String title = dto.getTitle();
        Long id = dto.getId();
        // 检查菜单是否存在
        this.existsById(id);
        // 标题查重
        if (QueryChain.of(mapper)
                .where(MENU.TITLE.eq(title))
                .and(MENU.ID.ne(id))
                .exists()) {
            throw new ServiceException(ResultCode.TITLE_REPEAT, "标题" + title + "重复");
        }
        // 组装实体类
        Menu menu = mapper.selectOneById(dto.getId());
        BeanUtil.copyProperties(dto, menu);
        menu.setUpdated(LocalDateTime.now());
        //更新DB
        if (!UpdateChain.of(menu)
                .where(MENU.ID.eq(menu.getId()))
                .update()) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库修改失败");
        }
        return true;

    }

    @Transactional
    @Override
    public boolean delete(Long id) {
        // 检查菜单是否存在
        this.existsById(id);
        // 查询父菜单ID和全部子菜单ID
        List<Long> deleteIds = QueryChain.of(mapper)
                .select(MENU.ID)
                .where(MENU.PID.eq(id))
                .or(MENU.ID.eq(id))
                .objListAs(Long.class);
        // 删除中间表
        UpdateChain.of(roleMenuMapper)
                .where(ROLE_MENU.FK_MENU_ID.in(deleteIds))
                .remove();
        //删除本表
        if (!UpdateChain.of(mapper)
                .where(MENU.ID.in(deleteIds))
                .remove()) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库删除失败");
        }
        return true;
    }

    @Override
    public boolean deleteBatch(List<Long> ids) {
        // 检查菜单是否存在
        if (QueryChain.of(mapper)
                .where(MENU.ID.in(ids))
                .count() < ids.size()) {
            throw new ServiceException(ResultCode.MENU_NOT_FOUND, "至少一个菜单数据不存在");
        }

        // 查询父菜单ID和全部子菜单ID
        List<Long> deleteIds = QueryChain.of(mapper)
                .select(MENU.ID)
                .where(MENU.PID.in(ids))
                .or(MENU.ID.in(ids))
                .objListAs(Long.class);

        // 删除中间表
        UpdateChain.of(roleMenuMapper)
                .where(ROLE_MENU.FK_MENU_ID.in(deleteIds))
                .remove();

        // 删除 MENU 表中的菜单
        if (!UpdateChain.of(mapper)
                .where(MENU.ID.in(deleteIds))
                .remove()) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库删除失败");
        }
        return true;
    }

    @Override
    public List<Long> listMenuIdsByRoleId(Long roleId) {
        return QueryChain.of(roleMenuMapper)
                .select(ROLE_MENU.FK_MENU_ID)
                .where(ROLE_MENU.FK_ROLE_ID.eq(roleId))
                .objListAs(Long.class);
    }

    @Transactional
    @Override
    public boolean updateMenusByRoleId(Long roleId, List<Long> menuIds) {
        //删除中间表 --> 角色和菜单的中间表 删除这个角色目前所有的菜单
        UpdateChain.of(roleMenuMapper)
                .where(ROLE_MENU.FK_ROLE_ID.eq(roleId))
                .remove();
        //如果提交过来的菜单列表为空 不做处理
        if (CollUtil.isEmpty(menuIds)) {
            return true;
        }
        //添加中间表 --> 角色和菜单的中间表 将当前传入过来的菜单列表 批量加入中间表
        List<RoleMenu> roleMenuList = new ArrayList<>();
        for (Long menuId : menuIds) {
            RoleMenu roleMenu = new RoleMenu();
            roleMenu.setFkRoleId(roleId);
            roleMenu.setFkMenuId(menuId);
            roleMenu.setCreated(LocalDateTime.now());
            roleMenu.setUpdated(LocalDateTime.now());
            roleMenuList.add(roleMenu);
        }
        if (roleMenuMapper.insertBatch(roleMenuList) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库批量更新菜单失败");
        }
        return true;
    }

    //检查菜单是否存在
    private void existsById(Long id) {
        if (!QueryChain.of(mapper)
                .where(MENU.ID.eq(id))
                .exists()) {
            throw new ServiceException(ResultCode.MENU_NOT_FOUND, id + "号菜单数据不存在");
        }
    }
}
