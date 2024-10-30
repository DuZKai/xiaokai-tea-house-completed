package com.xiaokai.service;

import com.xiaokai.dto.EmployeeDTO;
import com.xiaokai.dto.EmployeeLoginDTO;
import com.xiaokai.dto.EmployeePageQueryDTO;
import com.xiaokai.entity.Employee;
import com.xiaokai.result.PageResult;

public interface EmployeeService {

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    /**
     * 新增员工
     * @param employeeDTO
     */
    void save(EmployeeDTO employeeDTO);

    /**
     * 分页查询员工
     * @param employeePageQueryDTO
     * @return
     */
    PageResult pageQuery(EmployeePageQueryDTO employeePageQueryDTO);

    /**
     * 启用或停用员工
     * @param status
     * @param id
     */
    void startOrStop(Integer status, Long id);

    Employee getById(Long id);

    void update(EmployeeDTO employeeDTO);
}
