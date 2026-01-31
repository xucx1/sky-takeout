package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import org.apache.poi.util.PackageHelper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        // 后期需要进行md5加密，然后再进行比对
        // DigestUtils.md5DigestAsHex()是spring提供的一个方法。传入的pwd需要转为Bytes数组
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }

    /**
     * 新增员工
     * @param employeeDTO
     */
    @Override
    public void save(EmployeeDTO employeeDTO) {
        Employee employee = new Employee();
        // 对象属性拷贝，将所有属性一次性拷贝过来
        BeanUtils.copyProperties(employeeDTO, employee);    // DTO属性 -> 实体属性

        // 设置账号的状态，默认为正常
        employee.setStatus(StatusConstant.ENABLE);

        // 设置密码，默认密码是123456
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));

        // 设置创建时间和修改时间
//        employee.setCreateTime(LocalDateTime.now());
//        employee.setUpdateTime(LocalDateTime.now());

        // 设置当前记录创建人id和修改人id
//        employee.setCreateUser(BaseContext.getCurrentId());
//        employee.setUpdateUser(BaseContext.getCurrentId());

        // 调用持久层
        employeeMapper.insert(employee);
    }

    /**
     * 分页查询
     * @param employeePageQueryDTO
     * @return 分页查询封装类
     */
    @Override
    public PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        /*
         底层通过MySQL的limit关键字来实现分页查询
         select * from employee limit 0,10

         使用pagehelper插件来简化代码编写，该插件在根目录下的pom.xml已经导入(66-70)
         pagehelper底层是基于mybatis的拦截器实现，类似于mybatis的动态sql，动态地把limit关键字拼接并计算参数
         */
        // 开始分页查询，startPage帮我们完成字符串的拼接操作(具体细节看Day02-07)
        PageHelper.startPage(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());

        // 返回值类型要符合插件的要求，Page是从pagehelper导入的
        Page<Employee> page = employeeMapper.pageQuery(employeePageQueryDTO);

        // 把page对象加工处理成PageResult对象
        long total = page.getTotal();
        List<Employee> records = page.getResult();

        return new PageResult(total, records);
    }

    /**
     * 启用或禁用员工账号：根据传入的id，修改employee表里的status字段
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        // update employee set status = ? where id = ?
        // 为了通用性，sql语句应该写成动态的：不只是修改status，根据传入的参数的不同，还可以修改多个字段
        // 考虑到动态更新 -> 创建employee实体类
        /*Employee employee = new Employee();
        employee.setId(id);
        employee.setStatus(status);
         */
        // 通过Builder注解构造对象，与上面的方法效果完全一样。
        Employee employee = Employee.builder()
                .status(status)
                .id(id)
                .build();
        employeeMapper.update(employee);
    }

    /**
     * 根据id查询员工信息
     * @param id
     * @return
     */
    @Override
    public Employee getById(Long id) {
        Employee employee = employeeMapper.getById(id);
        // 将密码加工，不展示密码，加强安全性
        employee.setPassword("****");
        return employee;
    }

    /**
     * 编辑员工信息
     * @param employeeDTO
     */
    @Override
    public void update(EmployeeDTO employeeDTO) {
        // 由于接收的是DTO对象，而update方法要求employee对象，因此先进行对象的属性拷贝
        Employee employee = new Employee();
        // 对象属性拷贝，将所有属性一次性拷贝过来
        BeanUtils.copyProperties(employeeDTO, employee);    // DTO属性 -> 实体属性
        // 由于是修改操作，因此要设置修改时间和修改人
//        employee.setUpdateTime(LocalDateTime.now());
        // 每次请求都要经过拦截器处理，在拦截器中已经把当前的id设置好了，可以直接获取（即登录人的id）
//        employee.setUpdateUser(BaseContext.getCurrentId());
        employeeMapper.update(employee);
    }
}
