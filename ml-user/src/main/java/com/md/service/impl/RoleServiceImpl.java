package com.md.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdcardUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.md.dto.*;
import com.md.entity.User;
import com.md.entity.UserRole;
import com.md.exception.ServiceException;
import com.md.mapper.RoleMenuMapper;
import com.md.mapper.UserRoleMapper;
import com.md.result.ResultCode;
import com.md.util.UserUtil;
import com.md.vo.PageVO;
import com.md.vo.RoleSimpleListVO;
import com.md.vo.UserSimpleListVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.md.entity.Role;
import com.md.mapper.RoleMapper;
import com.md.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.md.entity.table.RoleMenuTableDef.ROLE_MENU;
import static com.md.entity.table.RoleTableDef.ROLE;
import static com.md.entity.table.UserRoleTableDef.USER_ROLE;
import static com.md.entity.table.UserTableDef.USER;

/**
 * 角色表 服务层实现。
 *
 * @author CM
 * @since v1.0.0
 */
@Service
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role>  implements RoleService{
    @Autowired
    private RoleMenuMapper roleMenuMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;

    @Override
    public boolean insert(RoleInsertDTO dto) {
        //标题查重
        if (QueryChain.of(mapper)
                .where(ROLE.TITLE.eq(dto.getTitle()))
                .exists()) {
            throw new ServiceException(ResultCode.TITLE_REPEAT, "角色标题" + dto.getTitle() + "已存在");
        }
        //组装实体类
        Role role = BeanUtil.copyProperties(dto, Role.class);
        role.setInfo(StrUtil.isEmpty(dto.getInfo()) ? "暂无描述" : dto.getInfo());
        role.setCreated(LocalDateTime.now());
        role.setUpdated(LocalDateTime.now());
        //DB添加
        if (mapper.insert(role) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库添加失败");
        }
        return true;
    }

    @Override
    public Role select(Long id) {
        Role role = mapper.selectOneById(id);
        if (ObjectUtil.isNull(role)) {
            throw new ServiceException(ResultCode.ROLE_NOT_FOUND, id + "号角色数据不存在");
        }
        return role;
    }

    @Override
    public List<RoleSimpleListVO> simpleList() {
        return QueryChain.of(mapper)
                .orderBy(ROLE.IDX.asc(), ROLE.ID.desc())
                .listAs(RoleSimpleListVO.class);
    }

    @Override
    public PageVO<Role> page(RolePageDTO dto) {
        QueryChain<Role> queryChain = QueryChain.of(mapper)
                .orderBy(ROLE.IDX.asc(), ROLE.ID.desc());

        // title条件
        String title = dto.getTitle();
        if (ObjectUtil.isNotNull(title)) {
            queryChain.where(ROLE.TITLE.like(title));
        }

        // DB分页并转为VO
        Page<Role> result = queryChain.withRelations().page(new Page<>(dto.getPageNum(), dto.getPageSize()));
        PageVO<Role> pageVO = new PageVO<>();
        BeanUtil.copyProperties(result, pageVO);
        pageVO.setPageNum(result.getPageNumber());
        return pageVO;
    }


    @Override
    public boolean update(RoleUpdateDTO dto) {
        //检查角色是否存在
        this.existsById(dto.getId());
        //标题查重
        if (QueryChain.of(mapper)
                .where(ROLE.TITLE.eq(dto.getTitle()))
                .exists()) {
            throw new ServiceException(ResultCode.TITLE_REPEAT, "角色标题" + dto.getTitle() + "已存在");
        }
        //组装实体类
        Role role = mapper.selectOneById(dto.getId());
        BeanUtil.copyProperties(dto, role);
        role.setUpdated(LocalDateTime.now());
        //执行DB修改
        if(mapper.update(role) <= 0){
            throw new ServiceException(ResultCode.MYSQL_ERROR,"数据库修改失败");
        }

        return true;
    }

    @Transactional
    @Override
    public boolean delete(Long id) {
        //检查角色是否存在
        this.existsById(id);
        //删除中间表 --> 角色和菜单关系表
        UpdateChain.of(roleMenuMapper)
                .where(ROLE_MENU.FK_ROLE_ID.eq(id))
                .remove();
        //删除中间表 --> 用户和角色关系表
        UpdateChain.of(userRoleMapper)
                .where(USER_ROLE.FK_ROLE_ID.eq(id))
                .remove();
        //删除本表
        if(mapper.deleteById(id) <= 0){
            throw new ServiceException(ResultCode.MYSQL_ERROR,"数据库删除失败");
        }

        return true;
    }

    @Override
    public boolean deleteBatch(List<Long> ids) {
        // 检查角色是否存在
        if (QueryChain.of(mapper)
                .where(ROLE.ID.in(ids))
                .count() < ids.size()) {
            throw new ServiceException(ResultCode.ROLE_NOT_FOUND, "至少一个角色数据不存在");
        }
        // 删除中间表
        UpdateChain.of(roleMenuMapper)
                .where(ROLE_MENU.FK_ROLE_ID.in(ids))
                .remove();
        UpdateChain.of(userRoleMapper)
                .where(USER_ROLE.FK_ROLE_ID.in(ids))
                .remove();

        // 删除基本表
        if (mapper.deleteBatchByIds(ids) != ids.size()) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库删除失败");
        }
        return true;
    }

    @Override
    public List<Long> listRoleIdsByUserId(Long userId) {
        //查询用户和角色的关系表 条件是 fk_user_id 返回的是 fk_role_id
        return  QueryChain.of(userRoleMapper)
                .select(USER_ROLE.FK_ROLE_ID)
                .where(USER_ROLE.FK_USER_ID.eq(userId))
                .objListAs(Long.class);
    }

    @Transactional
    @Override
    public boolean updateRolesByUserId(Long userId, List<Long> roleIds) {
        //删除用户角色-->删除的是用户和角色中间表的数据 把这个用户当前的角色全部删除
        UpdateChain.of(userRoleMapper)
                .where(USER_ROLE.FK_USER_ID.eq(userId))
                .remove();
        //如果新角色列表为空 直接返回
        if(CollUtil.isEmpty(roleIds)){
            return true;
        }
        //添加用户角色-->在中间表批量添加这个用户角色数据
        List<UserRole> userRoleList = new ArrayList<>();
        for (Long roleId : roleIds) {
            UserRole userRole = new UserRole();
            userRole.setFkUserId(userId);
            userRole.setFkRoleId(roleId);
            userRole.setCreated(LocalDateTime.now());
            userRole.setUpdated(LocalDateTime.now());
            userRoleList.add(userRole);
        }
        if(userRoleMapper.insertBatch(userRoleList) <= 0){
            throw new ServiceException(ResultCode.MYSQL_ERROR,"数据库批量更新角色失败");
        }
        return true;
    }

    //根据主键检查角色是否存在
    private void existsById(Long id) {
        if (!QueryChain.of(mapper)
                .where(ROLE.ID.eq(id))
                .exists()) {
            throw new ServiceException(ResultCode.ROLE_NOT_FOUND, id + "角色不存在");
        }
    }

}
