package com.md.controller;

import com.md.dto.RoleInsertDTO;
import com.md.dto.RolePageDTO;
import com.md.dto.RoleUpdateDTO;
import com.md.vo.PageVO;
import com.md.vo.RoleSimpleListVO;
import com.mybatisflex.core.paginate.Page;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.md.entity.Role;
import com.md.service.RoleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;

/**
 * 角色表 控制层。
 *
 * @author CM
 * @since v1.0.0
 */
@RestController
@Tag(name = "角色表接口")
@RequestMapping("/api/v1/role")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @PostMapping("/insert")
    @Operation(summary = "新增 - 单条数据", description = "新增一条角色记录")
    public boolean save(@Validated @RequestBody RoleInsertDTO dto) {
        return roleService.insert(dto);
    }

    @Operation(summary = "查询 - 单条查询", description = "按主键查询一条角色记录")
    @GetMapping("/select/{id}")
    public Role select(@PathVariable("id") Long id) {
        return roleService.select(id);
    }

    @Operation(summary = "查询 - 简单列表", description = "查询全部角色记录，仅返回简单信息")
    @GetMapping("/simpleList")
    public List<RoleSimpleListVO> simpleList() {
        return roleService.simpleList();
    }

    @Operation(summary = "查询 - 分页查询", description = "分页查询角色记录")
    @GetMapping("/page")
    public PageVO<Role> page(@Validated @ParameterObject RolePageDTO dto) {
        return roleService.page(dto);
    }

    @Operation(summary = "修改 - 修改单条", description = "按主键修改记录")
    @PutMapping("/update")
    public boolean update(@Validated @RequestBody RoleUpdateDTO dto) {
        return roleService.update(dto);
    }

    @Operation(summary = "删除 - 删除单条", description = "按主键删除记录")
    @DeleteMapping("/delete/{id}")
    public boolean delete(@PathVariable("id") Long id) {
        return roleService.delete(id);
    }

    @Operation(summary = "删除 - 批量删除", description = "按主键批量删除记录")
    @DeleteMapping("/deleteBatch")
    public boolean deleteBatch(@RequestParam("ids") List<Long> ids) {
        return roleService.deleteBatch(ids);
    }

    @Operation(summary = "查询 - 用户角色ID列表", description = "按用户ID查询用户全部角色的ID列表")
    @GetMapping("/listRoleIdsByUserId/{userId}")
    public List<Long> listRoleIdsByUserId(@PathVariable("userId") Long userId) {
        return roleService.listRoleIdsByUserId(userId);
    }

    @Operation(summary = "修改 - 用户角色", description = "按用户ID修改用户的角色列表")
    @PutMapping("/updateRolesByUserId")
    public boolean updateRolesByUserId(@RequestParam("userId") Long userId, @RequestParam("roleIds") List<Long> roleIds) {
        return roleService.updateRolesByUserId(userId, roleIds);
    }
}
