package com.ecotea.api.req.employee;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Data
public class ChangePasswordReq implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "原密码必填")
    private String oldPassword;

    @NotBlank(message = "新密码必填")
    @Size(min = 6, max = 32, message = "新密码长度须为 6～32 位")
    private String newPassword;
}
