package com.ecotea.api.mapper.employee;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecotea.api.domain.employee.Employee;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmployeeMapper extends BaseMapper<Employee> {
}
