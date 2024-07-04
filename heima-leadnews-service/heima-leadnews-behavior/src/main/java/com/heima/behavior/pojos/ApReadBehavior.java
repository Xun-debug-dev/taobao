package com.heima.behavior.pojos;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * APP阅读行为表
 * </p>
 *
 * @author itheima
 */
@Data
@Document("ap_read_behavior")
public class ApReadBehavior implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 文章ID
     */
    private Long articleId;

    /**
     * 阅读次数
     */
    private Integer count;

    /**
     * 阅读时间单位秒
     */
    private Integer readDuration;

    /**
     * 阅读百分比
     */
    private Short percentage;

    /**
     * 文章加载时间
     */
    private Short loadDuration;

    /**
     * 创建时间
     */
    private Date createdTime;

    /**
     * 更新时间
     */
    private Date updatedTime;

}