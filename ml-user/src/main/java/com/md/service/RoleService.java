package com.md.service;

import com.md.dto.RoleInsertDTO;
import com.md.dto.RolePageDTO;
import com.md.dto.RoleUpdateDTO;
import com.md.vo.PageVO;
import com.md.vo.RoleSimpleListVO;
import com.mybatisflex.core.service.IService;
import com.md.entity.Role;

import java.util.List;

/**
 * 角色表 服务层。
 *
 * @author CM
 * @since v1.0.0
 */
public interface RoleService extends IService<Role> {
    //添加 - 单条数据
    boolean insert(RoleInsertDTO dto);

    //查询 - 单条数据
    Role select(Long id);

    //查询 - 简单列表
    List<RoleSimpleListVO> simpleList();

    //查询 - 条件分页
    PageVO<Role> page(RolePageDTO dto);

    //修改 - 单条数据
    boolean update(RoleUpdateDTO dto);

    //删除 - 单条数据
    boolean delete(Long id);

    //删除 - 多条数据
    boolean deleteBatch(List<Long> ids);

    //查询 - 用户的角色ID列表
    List<Long> listRoleIdsByUserId(Long userId);

    //修改 - 用户角色
    boolean updateRolesByUserId(Long userId, List<Long> roleIds);
}
