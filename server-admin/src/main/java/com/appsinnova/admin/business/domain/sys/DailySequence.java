package com.appsinnova.admin.business.domain.sys;

import lombok.Data;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Data
@Entity
@Table(name = "daily_sequence")
@EntityListeners(AuditingEntityListener.class)
public class DailySequence implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer bizType;
    private String bizDate;
    private Integer currentSeq;
    private Long updateTime;
    private Long createTime;
}
