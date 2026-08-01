package com.md.controller;

import com.md.dto.UserInsertDTO;
import com.md.dto.UserPageDTO;
import com.md.dto.UserUpdateDTO;
import com.md.dto.UserUpdatePasswordDTO;
import com.md.result.Result;
import com.md.util.EasyExcelUtil;
import com.md.vo.PageVO;
import com.md.vo.UserSimpleListVO;
import com.mybatisflex.core.paginate.Page;
import jakarta.servlet.http.HttpServletResponse;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.md.entity.User;
import com.md.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;

/**
 * 用户表 控制层。
 *
 * @author CM
 * @since v1.0.0
 */
@RestController
@Tag(name = "用户表接口")
@RequestMapping("/api/v1/user")
public class UserController {
    @Autowired
    private UserService userService;

    @Operation(summary = "新增 - 单条新增",description = "新增一条用户记录")
    @PostMapping("/insert")
    public boolean insert(@Validated @RequestBody UserInsertDTO dto){
        return userService.insert(dto);
    }

    @Operation(summary = "查询 - 单条查询",description = "按主键查询一条用户记录")
    @GetMapping("/select/{id}")
    public User select(@PathVariable("id") Long id){
        return userService.select(id);
    }

    @Operation(summary = "查询 - 简单列表",description = "查询全部用户记录，仅返回简单信息")
    @GetMapping("/simpleList")
    public List<UserSimpleListVO> simpleList(){
        return userService.simpleList();
    }

    @Operation(summary = "查询 - 条件分页",description = "根据条件（动态） 分页查询用户记录")
    @GetMapping("/page")
    public PageVO<User> page(@Validated @ParameterObject UserPageDTO dto){
        return userService.page(dto);
    }

    @Operation(summary = "修改 - 单条记录",description = "按主键修改一条用户记录")
    @PutMapping("/update")
    public boolean update(@Validated @RequestBody UserUpdateDTO dto){
        return userService.update(dto);
    }

    @Operation(summary = "删除 - 单条记录",description = "按主键删除一条用户记录（关联删除用户的角色）")
    @DeleteMapping("/delete/{id}")
    public boolean delete(@PathVariable("id") Long id){
        return userService.delete(id);
    }

    @Operation(summary = "删除 - 批量删除",description = "按主键批量删除用户记录（关联删除用户的角色）")
    @DeleteMapping("/deleteBatch")
    public boolean delete(@RequestParam("ids") List<Long> ids){
        return userService.deleteBatch(ids);
    }

    @Operation(summary = "修改 - 重置密码",description = "按主键重置用户密码为默认密码 重置成功后 返回默认密码")
    @PutMapping("/resetPassword/{id}")
    public Result<String> resetPassword(@PathVariable("id") Long id){
        return new Result<>(userService.resetPassword(id));
    }

    @Operation(summary = "修改 - 用户密码",description = "按主键重置用户的登录密码")
    @PutMapping("/updatePassword")
    public boolean updatePassword(@Validated @RequestBody UserUpdatePasswordDTO dto){
        return userService.updatePassword(dto);
    }

    @Operation(summary = "查询 - 报表打印",description = "打印用户相关的报表数据")
    @GetMapping("/excel")
    public void excel(HttpServletResponse response){
        EasyExcelUtil.download(response,"用户统计表",userService.getExcelData());
    }
}
