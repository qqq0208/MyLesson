package com.md.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdcardUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.md.constant.ML;
import com.md.dto.UserInsertDTO;
import com.md.dto.UserPageDTO;
import com.md.dto.UserUpdateDTO;
import com.md.dto.UserUpdatePasswordDTO;
import com.md.excel.UserExcelDTO;
import com.md.exception.ServiceException;
import com.md.mapper.UserRoleMapper;
import com.md.result.ResultCode;
import com.md.util.UserUtil;
import com.md.vo.PageVO;
import com.md.vo.UserSimpleListVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.md.entity.User;
import com.md.mapper.UserMapper;
import com.md.service.UserService;
import org.apache.commons.codec.net.QCodec;
import org.aspectj.weaver.IUnwovenClassFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.rowset.serial.SerialException;
import java.awt.image.BandCombineOp;
import java.rmi.ServerException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.md.entity.table.UserRoleTableDef.USER_ROLE;
import static com.md.entity.table.UserTableDef.USER;

/**
 * 用户表 服务层实现。
 *
 * @author CM
 * @since v1.0.0
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>  implements UserService{
    //用户和角色中间表
    @Autowired
    private UserRoleMapper userRoleMapper;


    @Override
    public boolean insert(UserInsertDTO dto) {
        String idcard = dto.getIdcard();
        //检查身份证号
        if (!IdcardUtil.isValidCard(idcard)) {
            throw new ServiceException(ResultCode.ID_CARD_ILLEGAL, "身份证号" + idcard + "错误");
        }
        //检查身份证号是否重复
        if (QueryChain.of(mapper)
                .where(USER.IDCARD.eq(idcard))
                .exists()) {
            throw new ServiceException(ResultCode.ID_CARD_REPEAT, "身份证号" + idcard + "重复");
        }
        //检查用户名（登录账号）是否重复
        String username = dto.getUsername();
        if (QueryChain.of(mapper)
                .where(USER.USERNAME.eq(username))
                .exists()) {
            throw new ServiceException(ResultCode.USERNAME_REPEAT, "登录账号" + username + "重复");
        }
        //检查手机号是否重复
        String phone = dto.getPhone();
        if (QueryChain.of(mapper)
                .where(USER.PHONE.eq(phone))
                .exists()) {
            throw new ServiceException(ResultCode.PHONE_REPEAT, "手机号" + phone + "重复");
        }
        //检查邮箱是否重复
        String email = dto.getEmail();
        if (QueryChain.of(mapper)
                .where(USER.EMAIL.eq(email))
                .exists()) {
            throw new ServiceException(ResultCode.EMAIL_REPEAT, "电子邮箱" + email + "重复");
        }
        //组装User实体
        User user = BeanUtil.copyProperties(dto, User.class);
        //设置默认值
        user.setNickname(RandomUtil.randomString(10));
        user.setGender(UserUtil.defaultGender(idcard));
        user.setAge(UserUtil.defaultAge(idcard));
        user.setZodiac(UserUtil.defaultZodiac(idcard));
        user.setAvatar(UserUtil.defaultAvatar(idcard));
        user.setProvince(UserUtil.defaultProvince(idcard));
        user.setInfo(StrUtil.isEmpty(dto.getInfo()) ? "该用户很懒，没留下任何描述。" : dto.getInfo());
        user.setCreated(LocalDateTime.now());
        user.setUpdated(LocalDateTime.now());
        //使用BCrypt指定盐加密密码
        user.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(10)));
        //执行添加
        if (mapper.insert(user) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库添加失败");
        }
        return true;
    }

    @Override
    public User select(Long id) {
        User user = mapper.selectOneById(id);
        if (ObjectUtil.isNull(user)) {
            throw new ServiceException(ResultCode.USER_NOT_FOUND, id + "号用户不存在");
        }
        //返回脱敏后用户的数据
        return UserUtil.desensitization(user);
    }

    @Override
    public List<UserSimpleListVO> simpleList() {
        return QueryChain.of(mapper)
                .withRelations()
                .listAs(UserSimpleListVO.class);
    }

    @Override
    public PageVO<User> page(UserPageDTO dto) {
        //创建条件构造器
        QueryChain<User> queryChain = QueryChain.of(mapper);
        //username条件
        String username = dto.getUsername();
        if (ObjectUtil.isNotNull(username)) {
            queryChain.where(USER.USERNAME.like(username));
        }
        //nickname条件
        String nickname = dto.getNickname();
        if (ObjectUtil.isNotNull(nickname)) {
            queryChain.where(USER.NICKNAME.like(nickname));
        }
        //phone条件
        String phone = dto.getPhone();
        if (ObjectUtil.isNotNull(phone)) {
            queryChain.where(USER.PHONE.eq(phone));
        }
        //分页查询
        Page<User> result = queryChain.withRelations().page(new Page<>(dto.getPageNum(), dto.getPageSize()));
        //结果转VO
        //1. 将当前分页的结果（数据集合）进行重新设置（对用户的相关信息进行脱敏）
        result.setRecords(UserUtil.desensitization(result.getRecords()));
        //2. 创建返回的Vo对象
        PageVO<User> pageVO = new PageVO<>();
        //3. 将result属性复制给VO
        BeanUtil.copyProperties(result, pageVO);
        //4. 因为Page<User>的当前页 ： pageNumber  PageVO当前页：pageNum 属性名称不一样 不会复制 重新设置
        pageVO.setPageNum(result.getPageNumber());
        //5. 返回Vo结果
        return pageVO;
    }

    @Override
    public boolean update(UserUpdateDTO dto) {
        //检查用户是否存在
        this.existsById(dto.getId());
        //邮箱查重
        String email = dto.getEmail();
        if (QueryChain.of(mapper)
                .where(USER.EMAIL.eq(email))
                .exists()) {
            throw new ServiceException(ResultCode.EMAIL_REPEAT, "电子邮箱" + email + "重复");
        }

        //组装实体类
        User user = mapper.selectOneById(dto.getId());
        BeanUtil.copyProperties(dto, user);
        user.setUpdated(LocalDateTime.now());
        //执行修改
        if (mapper.update(user) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库修改失败");
        }
        return true;
    }

    @Transactional
    @Override
    public boolean delete(Long id) {
        //检查用户是否存在
        this.existsById(id);
        //删除中间表的数据（删除用户的角色）
        UpdateChain.of(userRoleMapper)
                .where(USER_ROLE.FK_USER_ID.eq(id))
                .remove();
        //删除本表
        if (mapper.deleteById(id) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库修改失败");
        }
        return true;
    }

    @Override
    public boolean deleteBatch(List<Long> ids) {
        //检查用户是否存在
        if (QueryChain.of(mapper)
                .where(USER.ID.in(ids))
                .count() < ids.size()) {
            throw new ServiceException(ResultCode.USER_NOT_FOUND, "至少一个用户数据不存在");
        }
        //删除中间表的数据（删除用户的角色）
        UpdateChain.of(userRoleMapper)
                .where(USER_ROLE.FK_USER_ID.in(ids))
                .remove();
        //删除本表
        if (mapper.deleteBatchByIds(ids) != ids.size()) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库修改失败");
        }
        return false;
    }

    @Override
    public String resetPassword(Long id) {
        //检查用户是否存在
        this.existsById(id);
        if(!UpdateChain.of(mapper)
                .set(USER.PASSWORD,BCrypt.hashpw(ML.User.DEFAULT_PASSWORD, BCrypt.gensalt(10)))
                .where(USER.ID.eq(id))
                .update()){
            throw new ServiceException(ResultCode.MYSQL_ERROR,"数据库重置密码失败");
        }
        return ML.User.DEFAULT_PASSWORD;
    }

    @Override
    public boolean updatePassword(UserUpdatePasswordDTO dto) {
        Long id = dto.getId();
        //检查用户是否存在
        User user = mapper.selectOneById(id);
        if(ObjectUtil.isNull(user)){
            throw new ServiceException(ResultCode.USER_NOT_FOUND,id + "号用户数据不存在");
        }
        //判断旧密码是否正确
        if(!BCrypt.checkpw(dto.getOldPassword(),user.getPassword())){
            throw new ServiceException(ResultCode.OLD_PASSWORD_ILLEGAL,id + "号用户旧密码错误");
        }
        //修改新密码
        if(!UpdateChain.of(mapper)
                .set(USER.PASSWORD,BCrypt.hashpw(dto.getNewPassword(),BCrypt.gensalt(10)))
                .where(USER.ID.eq(id))
                .update()){
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库修改密码失败");
        }
        return true;
    }

    @Override
    public List<UserExcelDTO> getExcelData() {
        //查询全部用户记录
        List<User> users = mapper.selectAll();
        //处理用户记录数据
        List<UserExcelDTO> result = new ArrayList<>();
        users.forEach(user -> {
            //在属性复制之前 给用户信息脱敏
            UserUtil.desensitization(user);
            UserExcelDTO dto = BeanUtil.copyProperties(user,UserExcelDTO.class);
            dto.setGender(ML.User.genderFormat(user.getGender()));
            result.add(dto);
        });
        //返回用户记录数据
        return result;
    }

    //根据主键查询用户是否存在
    private void existsById(Long id) {
        if (!QueryChain.of(mapper)
                .where(USER.ID.eq(id))
                .exists()) {
            throw new ServiceException(ResultCode.USER_NOT_FOUND, id + "号用户不存在");
        }
    }
}
