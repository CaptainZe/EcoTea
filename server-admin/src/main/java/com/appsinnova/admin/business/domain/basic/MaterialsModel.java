package com.appsinnova.admin.business.domain.basic;

import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Entity
@Table(name="materials")
@EntityListeners(AuditingEntityListener.class)
@DynamicInsert
@DynamicUpdate
public class MaterialsModel implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    private String brand;
    private String year;
    private BigDecimal price;
    private String imageUrls;
    private String operator;
    private Long updateTime;
    private Long createTime;

    @Transient
    private String urlShow;
}