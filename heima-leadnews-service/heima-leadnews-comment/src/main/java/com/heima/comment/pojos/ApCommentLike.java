package com.heima.comment.pojos;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * APP评论信息点赞
 */
@Data
@Document("ap_comment_like")
public class ApCommentLike {

    /**
     * id
     */
    @Id
    private String id;

    /**
     * 用户ID
     */
    private Integer userId;

    /**
     * 评论id
     	如果此时对文章评论的点赞的话，那么此id就是文章评论的id
     	如果此时对评论的回复点赞的话，那么此id就是评论回复的id
     */
    private String targetId;

    /**
     * 创建时间
     */
    private Date createdTime;
}