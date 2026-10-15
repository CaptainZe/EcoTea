package com.ecotea.api.service.employee;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 员工 Redis 会话（JsonUtils snake_case 读写）。
 */
@Data
public class EmployeeSession implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long employeeId;
    private String username;
    private String realName;
    /** 工作店铺业务键；未选店为 null */
    private Long tenantId;
    private String tenantName;
    private List<String> permCodes = new ArrayList<>();
}
