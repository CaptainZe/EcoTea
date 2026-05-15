package com.appsinnova.admin.business.domain.tea;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Table(name = "tea_partner")
@EntityListeners(AuditingEntityListener.class)
@DynamicInsert
@DynamicUpdate
public class TeaPartner implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long liaisonUserId;

    private Integer partnerType;
    private String partnerName;

    private String contactName;
    private String contactPhone;

    private String province;
    private String city;
    private String address;

    private String remark;
    private Integer status;
    private String operator;
    private Long updateTime;
    private Long createTime;

    /** 列表展示：关联登录账号 */
    @Transient
    private String linkedUserShow;

    /** 列表展示：对接客服 */
    @Transient
    private String liaisonUserShow;

    /** 查询：是否关联登录账号（YES_OR_NO：0-否 1-是） */
    @Transient
    private Integer userLinked;
}
