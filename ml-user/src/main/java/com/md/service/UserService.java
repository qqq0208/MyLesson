package com.md.service;


import com.md.dto.*;
import com.md.excel.UserExcelDTO;
import com.md.vo.LoginVO;
import com.md.vo.PageVO;
import com.md.vo.UserSimpleListVO;
import com.mybatisflex.core.service.IService;
import com.md.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 用户表 服务层。
 *
 * @author CM
 * @since v1.0.0
 */
public interface UserService extends IService<User> {
    //添加一个用户
    boolean insert(UserInsertDTO dto);

    //根据主键查询
    User select(Long id);

    //查询全部（简单查询）
    List<UserSimpleListVO> simpleList();

    //分页条件查询
    PageVO<User> page(UserPageDTO dto);

    //修改用户
    boolean update(UserUpdateDTO dto);

    //删除一个用户
    boolean delete(Long id);

    //删除多个用户
    boolean deleteBatch(List<Long> ids);

    //重置用户密码
    String resetPassword(Long id);

    //修改用户密码
    boolean updatePassword(UserUpdatePasswordDTO dto);

    //下载数据报表
    List<UserExcelDTO> getExcelData();

    //上传用户头像
    String uploadAvatar(MultipartFile newFile, Long id);

    //获取旧手机号的解绑验证码
    String getUnboundVCode(Long id);

    //校验旧手机号码的验证码
    boolean checkUnboundVCode(Long id, String vcode);

    //获取新手机号绑定验证码
    String getBoundVCode(String phone);

    //修改手机号
    boolean updatePhone(UserUpdatePhoneDTO dto);

    //账号密码登录
    LoginVO loginByAccount(LoginByAccountDTO dto);

    //获取登录验证码
    String getVcode(String phone);

    //手机号验证码登录
    LoginVO loginByPhone(LoginByPhoneDTO dto);

    //获取用户统计数据
    Map<String, Object> statistics();

}
