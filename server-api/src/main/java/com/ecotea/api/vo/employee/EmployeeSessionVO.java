package com.ecotea.api.vo.employee;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class EmployeeSessionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long employeeId;
    private String username;
    private String realName;
    private boolean tenantSelected;
    private Long tenantId;
    private String tenantName;
    private List<String> permCodes = new ArrayList<>();
    /** 是否已绑定小程序微信 */
    private boolean wxBound;
}
