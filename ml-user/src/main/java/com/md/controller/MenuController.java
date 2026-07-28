package com.md.controller;

import com.mybatisflex.core.paginate.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import com.md.entity.Menu;
import com.md.service.MenuService;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

    /**
     * 添加菜单表。
     *
     * @param menu 菜单表
     * @return {@code true} 添加成功，{@code false} 添加失败
     */
    @PostMapping("save")
    @Operation(description="保存菜单表")
    public boolean save(@RequestBody @Parameter(description="菜单表")Menu menu) {
        return menuService.save(menu);
    }

    /**
     * 根据主键删除菜单表。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    @Operation(description="根据主键菜单表")
    public boolean remove(@PathVariable @Parameter(description="菜单表主键")Long id) {
        return menuService.removeById(id);
    }

    /**
     * 根据主键更新菜单表。
     *
     * @param menu 菜单表
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("update")
    @Operation(description="根据主键更新菜单表")
    public boolean update(@RequestBody @Parameter(description="菜单表主键")Menu menu) {
        return menuService.updateById(menu);
    }

    /**
     * 查询所有菜单表。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    @Operation(description="查询所有菜单表")
    public List<Menu> list() {
        return menuService.list();
    }

    /**
     * 根据菜单表主键获取详细信息。
     *
     * @param id 菜单表主键
     * @return 菜单表详情
     */
    @GetMapping("getInfo/{id}")
    @Operation(description="根据主键获取菜单表")
    public Menu getInfo(@PathVariable Long id) {
        return menuService.getById(id);
    }

    /**
     * 分页查询菜单表。
     *
     * @param page 分页对象
     * @return 分页对象
     */
    @GetMapping("page")
    @Operation(description="分页查询菜单表")
    public Page<Menu> page(@Parameter(description="分页信息")Page<Menu> page) {
        return menuService.page(page);
    }

}
