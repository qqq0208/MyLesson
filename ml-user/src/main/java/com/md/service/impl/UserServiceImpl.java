package com.md.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdcardUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import cn.hutool.json.JSONUtil;
import com.md.component.MyRedis;
import com.md.constant.ML;
import com.md.dto.*;
import com.md.entity.Menu;
import com.md.excel.UserExcelDTO;
import com.md.exception.ServiceException;
import com.md.mapper.*;
import com.md.result.ResultCode;
import com.md.util.MinioUtil;
import com.md.util.UserUtil;
import com.md.vo.LoginVO;
import com.md.vo.PageVO;
import com.md.vo.UserSimpleListVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryChain;
import com.mybatisflex.core.query.QueryMethods;
import com.mybatisflex.core.update.UpdateChain;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.md.entity.User;
import com.md.service.UserService;
import org.apache.commons.codec.net.QCodec;
import org.aspectj.weaver.IUnwovenClassFile;
import org.bouncycastle.jcajce.provider.asymmetric.ec.KeyFactorySpi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.rowset.serial.SerialException;
import java.awt.image.BandCombineOp;
import java.rmi.ServerException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.md.entity.table.MenuTableDef.MENU;
import static com.md.entity.table.RoleMenuTableDef.ROLE_MENU;
import static com.md.entity.table.RoleTableDef.ROLE;
import static com.md.entity.table.UserRoleTableDef.USER_ROLE;
import static com.md.entity.table.UserTableDef.USER;
import static com.mybatisflex.core.query.QueryMethods.*;

/**
 * 用户表 服务层实现。
 *
 * @author CM
 * @since v1.0.0
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>  implements UserService {

    //用户和角色中间表
    @Autowired
    private UserRoleMapper userRoleMapper;
    //角色表
    @Autowired
    private RoleMapper roleMapper;
    //菜单表
    @Autowired
    private MenuMapper menuMapper;
    //角色和菜单中间表
    @Autowired
    private RoleMenuMapper roleMenuMapper;
    //注入Redis工具类
    @Autowired
    private MyRedis redis;


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
        if (!UpdateChain.of(mapper)
                .set(USER.PASSWORD, BCrypt.hashpw(ML.User.DEFAULT_PASSWORD, BCrypt.gensalt(10)))
                .where(USER.ID.eq(id))
                .update()) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库重置密码失败");
        }
        return ML.User.DEFAULT_PASSWORD;
    }

    @Override
    public boolean updatePassword(UserUpdatePasswordDTO dto) {
        Long id = dto.getId();
        //检查用户是否存在
        User user = mapper.selectOneById(id);
        if (ObjectUtil.isNull(user)) {
            throw new ServiceException(ResultCode.USER_NOT_FOUND, id + "号用户数据不存在");
        }
        //判断旧密码是否正确
        if (!BCrypt.checkpw(dto.getOldPassword(), user.getPassword())) {
            throw new ServiceException(ResultCode.OLD_PASSWORD_ILLEGAL, id + "号用户旧密码错误");
        }
        //修改新密码
        if (!UpdateChain.of(mapper)
                .set(USER.PASSWORD, BCrypt.hashpw(dto.getNewPassword(), BCrypt.gensalt(10)))
                .where(USER.ID.eq(id))
                .update()) {
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
            UserExcelDTO dto = BeanUtil.copyProperties(user, UserExcelDTO.class);
            dto.setGender(ML.User.genderFormat(user.getGender()));
            result.add(dto);
        });
        //返回用户记录数据
        return result;
    }

    @Override
    public String uploadAvatar(MultipartFile newFile, Long id) {
        //根据主键查询用户
        User user = mapper.selectOneById(id);
        if (ObjectUtil.isNull(user)) {
            throw new ServiceException(ResultCode.USER_NOT_FOUND, id + "号用户不存在");
        }
        //得到当前头像
        String avatar = user.getAvatar();
        //生成新文件名字
        String newFileName = MinioUtil.randomFilename(newFile);
        //更新DB
        user.setAvatar(newFileName);
        user.setUpdated(LocalDateTime.now());
        if (mapper.update(user) <= 0) {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库更新头像失败");
        }
        try {
            //MInIo删除旧文件（默认文件不删除）
            if (!ML.User.DEFAULT_AVATARS.contains(avatar)) {
                MinioUtil.delete(avatar, ML.MinIO.AVATAR_DIR, ML.MinIO.BUCKET_NAME);
            }
            //上传新文件
            MinioUtil.upload(newFile, newFileName, ML.MinIO.AVATAR_DIR, ML.MinIO.BUCKET_NAME);
        } catch (Exception e) {
            throw new ServiceException(ResultCode.SERVER_ERROR, "MinIo操作失败：" + e.getMessage());
        }
        //返回新文件名字
        return newFileName;
    }

    @Override
    public String getUnboundVCode(Long id) {
        //根据主键ID查询当前用户的手机号
        String phone = QueryChain.of(mapper)
                .select(USER.PHONE)
                .where(USER.ID.eq(id))
                .objAs(String.class);
        if (ObjectUtil.isNull(phone)) {
            throw new ServiceException(ResultCode.PHONE_NOT_FOUND, "手机号" + phone + "不存在");
        }
        //定义Redis的Key和value
        String key = ML.Redis.UNBOUND_VCODE_PREFIX + phone;
        String value = RandomUtil.randomNumbers(6);
        //将验证码存入Redis
        redis.setEx(key, value, 5, TimeUnit.MINUTES);
        //向指定手机号码发送短信（暂时忽略）

        //将验证码返回给前端
        return value;
    }

    @Override
    public boolean checkUnboundVCode(Long id, String vcode) {
        //根据主键ID查询当前用户的手机号
        String phone = QueryChain.of(mapper)
                .select(USER.PHONE)
                .where(USER.ID.eq(id))
                .objAs(String.class);
        if (ObjectUtil.isNull(phone)) {
            throw new ServiceException(ResultCode.PHONE_NOT_FOUND, "手机号" + phone + "不存在");
        }
        //从Redis中获取验证码
        String key = ML.Redis.UNBOUND_VCODE_PREFIX + phone;
        String redisVcode = redis.get(key);
        if (ObjectUtil.isNull(redisVcode)) {
            throw new ServiceException(ResultCode.VCODE_ILLEGAL, "验证码" + redisVcode + "失效");
        }
        //校验验证码是否正确
        if (redisVcode.equals(vcode)) {
            //删除当前验证码
            redis.del(key);
        } else {
            throw new ServiceException(ResultCode.VCODE_ILLEGAL, "验证码" + redisVcode + "错误");
        }

        return true;
    }

    @Override
    public String getBoundVCode(String phone) {
        if (QueryChain.of(mapper)
                .select(USER.PHONE)
                .where(USER.PHONE.eq(phone))
                .exists()) {
            throw new ServiceException(ResultCode.PHONE_REPEAT, "手机号码" + phone + "重复");
        }
        //定义Redis的Key和value
        String key = ML.Redis.UNBOUND_VCODE_PREFIX + phone;
        String value = RandomUtil.randomNumbers(6);
        //将验证码存入Redis
        redis.setEx(key, value, 5, TimeUnit.MINUTES);
        //向指定手机号码发送短信（暂时忽略）

        //将验证码返回给前端
        return value;
    }

    @Override
    public boolean updatePhone(UserUpdatePhoneDTO dto) {
        Long id = dto.getId();
        String phone = dto.getPhone();
        String vcode = dto.getVcode();
        //检查用户是否存在
        this.existsById(id);
        //检查手机号码是否重复
        if (QueryChain.of(mapper)
                .select(USER.PHONE)
                .where(USER.PHONE.eq(phone))
                .exists()) {
            throw new ServiceException(ResultCode.PHONE_REPEAT, "手机号码" + phone + "重复");
        }
        //从Redis中获取验证码
        String key = ML.Redis.UNBOUND_VCODE_PREFIX + phone;
        String redisVcode = redis.get(key);
        if (ObjectUtil.isNull(redisVcode)) {
            throw new ServiceException(ResultCode.VCODE_ILLEGAL, "验证码" + redisVcode + "失效");
        }
        //校验验证码是否正确
        if (!redisVcode.equals(vcode)) {
            throw new ServiceException(ResultCode.VCODE_ILLEGAL, "验证码" + redisVcode + "错误");
        }
        //修改用户手机号 修改成功后 删除redis中的验证码
        if (UpdateChain.of(mapper)
                .set(USER.PHONE, phone)
                .where(USER.ID.eq(id))
                .update()) {
            redis.del(key);
        } else {
            throw new ServiceException(ResultCode.MYSQL_ERROR, "数据库修改手机号失败");
        }
        return true;
    }

    @Override
    public LoginVO loginByAccount(LoginByAccountDTO dto) {
        String username = dto.getUsername();
        String password = dto.getPassword();
        //根据账号查询用户记录
        User user = QueryChain.of(mapper)
                .where(USER.USERNAME.eq(username))
                .one();
        if (ObjectUtil.isNull(user)) {
            throw new ServiceException(ResultCode.ACCOUNT_ILLEGAL, "账号" + username + "不存在");
        }
        //校验密码是否匹配
        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new ServiceException(ResultCode.ACCOUNT_ILLEGAL, "密码" + password + "错误");
        }
        //返回构建的数据
        return buildLoginVo(user);
    }

    @Override
    public String getVcode(String phone) {
        //检查手机号是否存在
        if (!QueryChain.of(mapper)
                .where(USER.PHONE.eq(phone))
                .exists()) {
            throw new ServiceException(ResultCode.PHONE_NOT_FOUND, "手机号码" + phone + "不存在");
        }
        //生成验证码 并且存入Redis
        String key = ML.Redis.LOGIN_VCODE_PREFIX + phone;
        String val = RandomUtil.randomNumbers(6);
        redis.setEx(key, val, 5, TimeUnit.MINUTES);
        //返回
        return val;
    }

    @Override
    public LoginVO loginByPhone(LoginByPhoneDTO dto) {
        String phone = dto.getPhone();
        String vcode = dto.getVcode();
        //检查手机号是否存在
        User user = QueryChain.of(mapper)
                .where(USER.PHONE.eq(phone))
                .one();
        if (ObjectUtil.isNull(user)) {
            throw new ServiceException(ResultCode.PHONE_NOT_FOUND, "手机号" + phone + "不存在");
        }
        //校验验证码
        String key = ML.Redis.LOGIN_VCODE_PREFIX + phone;
        String redisCode = redis.get(key);
        if (ObjectUtil.isNull(redisCode) || !redisCode.equals(vcode)) {
            throw new ServiceException(ResultCode.VCODE_ILLEGAL, "验证码" + vcode + "无效");
        }
        //删除验证码
        redis.del(key);
        //返回构建的数据
        return buildLoginVo(user);
    }

    @Override
    public Map<String, Object> statistics() {
        //创建最终返回的Map集合
        Map<String, Object> result = new HashMap<>();

        //1. 设置用户性别比例数据
        List<Map> genderMap = QueryChain.of(mapper)
                .select(USER.GENDER.as("name"), QueryMethods.count().as("value"))
                .groupBy(USER.GENDER)
                .orderBy(USER.GENDER.asc())
                .listAs(Map.class);
        result.put("genderCount", genderMap);

        //2. 设置今日注册用户数量
        long todayCount = QueryChain.of(mapper)
                .where(dateDiff(currentDate(), dateFormat(USER.CREATED, "%Y-%m-%d")).eq(0))
                .count();
        result.put("todayCount", todayCount);

        //3. 设置昨日注册用户数量
        long yesterdayCount = QueryChain.of(mapper)
                .where(dateDiff(currentDate(), dateFormat(USER.CREATED, "%Y-%m-%d")).eq(1))
                .count();
        result.put("yesterdayCount", yesterdayCount);

        //4. 设置今年的用户数量
        long thisYearCount = QueryChain.of(mapper)
                .where(year(USER.CREATED).eq(year(currentDate())))
                .count();
        result.put("thisYearCount", thisYearCount);

        //5. 设置去年的用户数量
        LocalDate lastYearStart = LocalDate.now().minusYears(1).withDayOfYear(1);
        LocalDate thisYearStart = LocalDate.now().withDayOfYear(1);
        long lastYearCount = QueryChain.of(mapper)
                .where(USER.CREATED.ge(lastYearStart).getColumn().lt(thisYearStart))
                .count();
        result.put("lastYearCount", lastYearCount);

        //6. 设置日增长率
        result.put("dayIncrease", increase(todayCount, yesterdayCount));

        //7. 设置年增长率
        result.put("yearIncrease", increase(yesterdayCount, lastYearCount));

        //返回结果集合
        return result;
    }

    //计算a到b的增长率
    private String increase(double a, double b) {
        if (b == 0) {
            return a > b ? "100.00" : a < b ? "-100" : "0";
        }
        return String.format("%.2f", (a - b) / b);
    }

    //构建登录成功后返回的Vo数据
    private LoginVO buildLoginVo(User user) {
        LoginVO result = new LoginVO();
        String tokenKey = UUID.randomUUID().toString();
        redis.setEx(tokenKey, JSONUtil.toJsonStr(user), 30, TimeUnit.MINUTES);
        //查询角色ID列表 --> 查询的是UserRole 中间关系表
        List<Long> roleIds = QueryChain.of(userRoleMapper)
                .select(USER_ROLE.FK_ROLE_ID)
                .where(USER_ROLE.FK_USER_ID.eq(user.getId()))
                .objListAs(Long.class);
        //如果用户没有角色 直接返回对应信息
        if (CollUtil.isEmpty(roleIds)) {
            result.setRoleTitles(null);
            result.setMenus(null);
            result.setUser(UserUtil.desensitization(user));
            result.setToken(tokenKey);
            return result;
        }
        //根据角色ID的集合 查询角色表 只返回角色标题
        List<String> roleTitles = QueryChain.of(roleMapper)
                .select(ROLE.TITLE)
                .where(ROLE.ID.in(roleIds))
                .objListAs(String.class);
        //根据角色ID的集合 查询角色和菜单中间关系表 返回菜单ID的集合
        List<Long> meunIds = QueryChain.of(roleMenuMapper)
                .select(ROLE_MENU.FK_MENU_ID)
                .where(ROLE_MENU.FK_ROLE_ID.in(roleIds))
                .objListAs(Long.class);
        //判断角色是否有菜单
        if (CollUtil.isEmpty(meunIds)) {
            result.setRoleTitles(roleTitles);
            result.setMenus(null);
            result.setUser(UserUtil.desensitization(user));
            result.setToken(tokenKey);
            return result;
        }
        //根据菜单ID的集合 查询菜单表 返回菜单数据 （只查询父菜单）
        List<Menu> menuList = QueryChain.of(menuMapper)
                .where(MENU.ID.in(meunIds))
                .and(MENU.PID.eq(ML.Menu.ROOT_ID))
                .orderBy(MENU.IDX.asc(), MENU.ID.desc())
                .list();

        //构建完整的返回数据
        result.setRoleTitles(roleTitles);
        result.setMenus(menuList);
        result.setUser(UserUtil.desensitization(user));
        result.setToken(tokenKey);
        //返回结果对象
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
