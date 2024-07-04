package com.heima.comment.pojos;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * APP评论信息
 */
@Data
@Document("ap_comment")
public class ApComment {

    /**
     * id
     */
    @Id
    private String id;

    /**
     * 登录用户ID
     */
    private Integer userId;

    /**
     * 用户昵称
     */
    private String nickName;
    
	/**
     * 用户头像
     */
    private String image;

    /**
     * 评论目标id
     	有可能是文章，视频，还有可能是评论id
     */
    private String targetId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 点赞数
     */
    private Integer likes;

    /**
     * 回复数
     */
    private Integer reply;

    /**
     * 创建时间
     */
    private Date createdTime;

    /**
     * 更新时间
     */
    private Date updatedTime;

}