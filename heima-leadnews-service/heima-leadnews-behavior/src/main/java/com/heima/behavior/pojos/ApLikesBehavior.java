package com.heima.behavior.pojos;


import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * APP点赞行为表
 * </p>
 *
 * @author itheima
 */
@Data
@Document("ap_likes_behavior")
public class ApLikesBehavior implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    private String id;

    /**
     * 登录用户ID
     */
    private Long userId;

    /**
     * 文章ID
     	注意：当前如果对文章进行点赞的话，那么这个id就是文章id
     	如果对视频进行点赞的话，那么这个id就是视频的id
     */
    private Long articleId;

    /**
     * 点赞内容类型
     * 0文章
     * 1动态（视频或者音频等）
     */
    private Short type;

    /**
     * 创建时间
     */
    private Date createdTime;
}