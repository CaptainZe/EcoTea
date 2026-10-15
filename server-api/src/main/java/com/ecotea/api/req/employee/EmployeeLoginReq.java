package com.ecotea.api.req.employee;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Data
public class EmployeeLoginReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "登录名必填")
    private String username;

    @NotBlank(message = "密码必填")
    private String password;
}
