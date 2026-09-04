package com.appsinnova.admin.business.domain.wx;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * 微信订阅号自定义菜单 wx_mp_menu。
 */
@Data
@Entity
@Table(name = "wx_mp_menu")
@EntityListeners(AuditingEntityListener.class)
@DynamicInsert
@DynamicUpdate
public class WxMpMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String appId;
    private String appName;

    /** 微信 menu/create JSON */
    @Lob
    private String config;

    private String operator;
    private Long updateTime;
    private Long createTime;
}
