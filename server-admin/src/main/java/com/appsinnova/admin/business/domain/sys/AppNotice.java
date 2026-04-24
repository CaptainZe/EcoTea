package com.appsinnova.admin.business.domain.sys;

import lombok.Data;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;

@Data
@Entity
@Table(name = "app_notice")
@EntityListeners(AuditingEntityListener.class)
public class AppNotice implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer type;
    private String content;
    private String operator;
    private Long updateTime;
    private Long createTime;
}