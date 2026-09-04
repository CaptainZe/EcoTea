package com.appsinnova.admin.business.domain.wx;

import com.appsinnova.admin.business.vo.wx.WxCustomerServiceConfig;
import com.appsinnova.admin.business.vo.wx.WxRecycleDescConfig;
import com.appsinnova.admin.business.vo.wx.WxSaleH5CopyConfig;
import com.appsinnova.admin.business.vo.wx.WxSubscribeWelcomeConfig;
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
import javax.persistence.Transient;
import java.io.Serializable;

@Data
@Entity
@Table(name = "wx_global_config")
@EntityListeners(AuditingEntityListener.class)
@DynamicInsert
@DynamicUpdate
public class WxGlobalConfig implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 见 WxGlobalConfigType */
    private Integer type;

    /** 扁平 JSON 字符串 */
    @Lob
    private String config;

    private String operator;
    private Long updateTime;
    private Long createTime;

    /** 列表展示摘要（不落库） */
    @Transient
    private String displayConfig;

    /** type=1 表单（不落库） */
    @Transient
    private WxSubscribeWelcomeConfig subscribeWelcomeConfig;

    /** type=2 表单（不落库） */
    @Transient
    private WxRecycleDescConfig recycleDescConfig;

    /** type=3 表单（不落库） */
    @Transient
    private WxCustomerServiceConfig customerServiceConfig;

    /** type=4 表单（不落库） */
    @Transient
    private WxSaleH5CopyConfig saleH5CopyConfig;
}
