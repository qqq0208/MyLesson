package com.md.controller;


import com.md.dto.MenuInsertDTO;
import com.md.dto.MenuPageDTO;
import com.md.dto.MenuUpdateDTO;
import com.md.vo.MenuSimpleListVO;
import com.md.vo.PageVO;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.md.entity.Menu;
import com.md.service.MenuService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;


import java.util.List;

/**
 * 菜单表 控制层。
 *
 * @author CM
 * @since v1.0.0
 */
@RestController
@Tag(name = "菜单表接口")
@RequestMapping("/api/v1/menu")
public class MenuController {
    @Autowired
    private MenuService menuService;

    @Operation(summary = "新增 - 单条新增", description = "新增一条菜单记录")
    @PostMapping("/insert")
    public boolean insert(@Validated @RequestBody MenuInsertDTO dto) {
        return menuService.insert(dto);
    }

    @Operation(summary = "查询 - 单条查询", description = "按主键查询一条菜单记录")
    @GetMapping("/select/{id}")
    public Menu select(@PathVariable("id") Long id) {
        return menuService.select(id);
    }

    @Operation(summary = "查询 - 简单列表", description = "查询全部菜单记录，仅返回简单信息")
    @GetMapping("/simpleList")
    public List<MenuSimpleListVO> simpleList() {
        return menuService.simpleList();
    }

    @Operation(summary = "查询 - 分页查询", description = "分页查询菜单记录")
    @GetMapping("/page")
    public PageVO<Menu> page(@Validated @ParameterObject MenuPageDTO dto) {
        return menuService.page(dto);
    }

    @Operation(summary = "修改 - 单条修改", description = "按主键修改一条菜单记录")
    @PutMapping("update")
    public boolean update(@Validated @RequestBody MenuUpdateDTO dto) {
        return menuService.update(dto);
    }

    @Operation(summary = "删除 - 单条删除", description = "按主键删除一条菜单记录")
    @DeleteMapping("delete/{id}")
    public boolean delete(@PathVariable("id") Long id) {
        return menuService.delete(id);
    }

    @Operation(summary = "删除 - 批量删除", description = "按主键批量删除菜单记录")
    @DeleteMapping("deleteBatch")
    public boolean deleteBatch(@RequestParam("ids") List<Long> ids) {
        return menuService.deleteBatch(ids);
    }

    @Operation(summary = "查询 - 角色菜单", description = "按角色Id查询菜单ID的列表")
    @GetMapping("/listMenuIdsByRoleId/{roleId}")
    public List<Long> listMenuIdsByRoleId(@PathVariable("roleId") Long roleId) {
        return menuService.listMenuIdsByRoleId(roleId);
    }

    @Operation(summary = "修改 - 角色菜单", description = "按角色Id修改角色菜单列表")
    @PutMapping("/updateRolesByUserId")
    public boolean updateRolesByUserId(@RequestParam("roleId") Long roleId, @RequestParam("menuIds") List<Long> menuIds) {
        return menuService.updateMenusByRoleId(roleId, menuIds);
    }

}

