package com.appsinnova.admin.business.domain.sys;

import lombok.Data;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Table(name = "app_secret_key")
@EntityListeners(AuditingEntityListener.class)
public class AppSecretKey implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer type;
    private String accessKey;
    private String accessSecret;
    private String remark;
    private String operator;
    private Long updateTime;
    private Long createTime;
}