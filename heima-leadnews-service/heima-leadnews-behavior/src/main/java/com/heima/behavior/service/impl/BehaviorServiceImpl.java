package com.heima.behavior.service.impl;
import com.alibaba.fastjson.JSON;
import com.heima.behavior.pojos.ApFollowBehavior;
import com.heima.behavior.pojos.ApLikesBehavior;
import com.heima.behavior.pojos.ApReadBehavior;
import com.heima.behavior.service.BehaviorService;
import com.heima.common.redis.RedisCacheService;
import com.heima.model.behavior.dtos.ArticleBehaviorDto;
import com.heima.model.behavior.dtos.FollowBehaviorDto;
import com.heima.model.behavior.dtos.LikesBehaviorDto;
import com.heima.model.behavior.dtos.ReadBehaviorDto;
import com.heima.model.common.constants.HotArticleConstants;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.message.UpdateArticleMess;
import com.heima.model.user.pojo.ApUser;
import com.heima.utils.threadlocal.AppThreadLocalUtil;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class BehaviorServiceImpl implements BehaviorService {

    @Autowired
    private RedisCacheService redisCacheService;

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 用户关注行为
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult userFollow(FollowBehaviorDto dto) {

        //0.非空判断
        if (dto.getAuthorId()==null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE,"当前作者为空，无法关注");
        }

        ApUser user = AppThreadLocalUtil.getUser();
        if(user==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"非法进入");
        }

        Integer userId = user.getId();
        if(userId==0){
            //游客身份
            return ResponseResult.errorResult(AppHttpCodeEnum.NO_OPERATOR_AUTH,"当前用户是游客身份,无权操作");
        }
        //定义redis中的key
        String followKey="USER_FOLLOW_"+userId;
        String fansKey="USER_FANS_"+dto.getAuthorId();

        //查询一下当前用户是否关注过该作者
        boolean flag = redisCacheService.hExists(followKey, dto.getAuthorId().toString());

        //如果已经关注过，并且正在执行的是关注操作
        if(flag && dto.getOperation()==0){
            //直接返回
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_EXIST,"当前用户已经关注过该作者");
        }
        //如果已经关注过，现在正在执行的取消关注操作
        if(flag && dto.getOperation()==1){
            //1.删除redis
            redisCacheService.hDelete(followKey,dto.getAuthorId().toString());
            redisCacheService.hDelete(fansKey,userId.toString());
            //2.删除mongo
            mongoTemplate.remove(Query.query(
                    Criteria.where("userId").is(userId).and("followId").is(dto.getAuthorId())
            ),ApFollowBehavior.class);
            return ResponseResult.okResult("取消关注成功");
        }
        //如果没有关注过，则正在执行的关注操作
        if(!flag && dto.getOperation()==0){
            //1.保存数据到redis中

            redisCacheService.hPut(followKey,dto.getAuthorId().toString(),"1");
            redisCacheService.hPut(fansKey,userId.toString(),"1");

            //2.保存数据到mongoDB中
            ApFollowBehavior apFollowBehavior=new ApFollowBehavior();
            apFollowBehavior.setId(ObjectId.get().toString());
            apFollowBehavior.setUserId(userId.longValue());
            apFollowBehavior.setArticleId(dto.getArticleId());
            apFollowBehavior.setFollowId(dto.getAuthorId());
            apFollowBehavior.setCreatedTime(new Date());
            mongoTemplate.save(apFollowBehavior);
            //3.响应数据
            return ResponseResult.okResult("关注成功");
        }
      return ResponseResult.errorResult(AppHttpCodeEnum.SERVER_ERROR);
    }

    /**
     * 点赞行为
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult like(LikesBehaviorDto dto) {

        //0.非空判断
        if (dto.getArticleId()==null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE,"当前文章id为空，无法点赞");
        }

        ApUser user = AppThreadLocalUtil.getUser();
        if(user==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"非法进入");
        }

        Integer userId = user.getId();
        if(userId==0){
            //游客身份
            return ResponseResult.errorResult(AppHttpCodeEnum.NO_OPERATOR_AUTH,"当前用户是游客身份,无权操作");
        }

        //查询当前用户是否点赞过该文章
        String likeKey="USER_LIKES_"+dto.getArticleId();
        boolean flag = redisCacheService.hExists(likeKey, userId.toString());

        //如果已经点赞过，并且正在执行的是点赞操作
        if(flag && dto.getOperation()==0){
            //直接返回
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_EXIST,"当前用户已经点赞过该文章");
        }
        //封装发布的消息内容
        UpdateArticleMess mess=new UpdateArticleMess();
        mess.setArticleId(dto.getArticleId());
        mess.setType(UpdateArticleMess.UpdateArticleType.LIKES);

        //如果已经点赞过，现在正在执行的取消点赞操作
        if(flag && dto.getOperation()==1){
            //1.删除redis
            redisCacheService.hDelete(likeKey,userId.toString());
            //2.删除mongo
            mongoTemplate.remove(Query.query(
                    Criteria.where("userId").is(userId).and("articleId").is(dto.getArticleId())
            ), ApLikesBehavior.class);

            mess.setAdd(-1);
            //发送消息
           // kafkaTemplate.send(HotArticleConstants.HOT_ARTICLE_SCORE_TOPIC, JSON.toJSONString(mess));

            return ResponseResult.okResult("取消点赞成功");
        }
        //如果没有点赞过，且正在执行的是点赞行为
        if(!flag && dto.getOperation()==0){
            //保存数据到redis
            redisCacheService.hPut(likeKey,userId.toString(),"1");
            //保存数据到mongo
            ApLikesBehavior apLikesBehavior=new ApLikesBehavior();
            apLikesBehavior.setId(ObjectId.get().toString());
            apLikesBehavior.setUserId(userId.longValue());
            apLikesBehavior.setArticleId(dto.getArticleId());
            apLikesBehavior.setType((short)0);
            apLikesBehavior.setCreatedTime(new Date());
            mongoTemplate.save(apLikesBehavior);

            mess.setAdd(1);
            //发送消息
            //kafkaTemplate.send(HotArticleConstants.HOT_ARTICLE_SCORE_TOPIC, JSON.toJSONString(mess));

            //3.响应数据
            return ResponseResult.okResult("点赞成功");
        }



        return ResponseResult.errorResult(AppHttpCodeEnum.SERVER_ERROR);
    }

    /**
     * 阅读行为
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult readBehavior(ReadBehaviorDto dto) {

        //0.非空判断
        if (dto.getArticleId()==null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE,"当前文章id为空，无法点赞");
        }

        //默认游客身份也可以记录阅读行为
        Integer userId = AppThreadLocalUtil.getUser().getId();
        //先根据条件查询mongo,然后判断到底是修改还是新增
        ApReadBehavior readBehavior = mongoTemplate.findOne(Query.query(
                Criteria.where("userId").is(userId).and("articleId").is(dto.getArticleId())
        ), ApReadBehavior.class);


        //没有查到数据，说明是新增
        if (readBehavior==null) {
            readBehavior =new ApReadBehavior();
            readBehavior.setId(ObjectId.get().toString());
            readBehavior.setUserId(userId.longValue());
            readBehavior.setArticleId(dto.getArticleId());
            readBehavior.setCount(dto.getCount().intValue());
            readBehavior.setReadDuration(dto.getReadDuration());
            readBehavior.setPercentage(dto.getPercentage());
            readBehavior.setLoadDuration(dto.getLoadDuration());
            readBehavior.setCreatedTime(new Date());
            readBehavior.setUpdatedTime(new Date());
        }else{
            //修改
            readBehavior.setCount(readBehavior.getCount()+dto.getCount());
            readBehavior.setUpdatedTime(new Date());
        }
        mongoTemplate.save(readBehavior);

        //封装发布的消息内容
        UpdateArticleMess mess=new UpdateArticleMess();
        mess.setArticleId(dto.getArticleId());
        mess.setType(UpdateArticleMess.UpdateArticleType.VIEWS);
        mess.setAdd(1);
        //发送消息
        //kafkaTemplate.send(HotArticleConstants.HOT_ARTICLE_SCORE_TOPIC, JSON.toJSONString(mess));

        return ResponseResult.okResult("阅读行为记录完成");
    }

    /**
     * 加载行为列表
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult loadArticleBehavior(ArticleBehaviorDto dto) {

        //1.非空判断
        if(dto.getArticleId()==null || dto.getAuthorId()==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE,"参数有问题");
        }

        //2.判断是否是游客身份
        ApUser user = AppThreadLocalUtil.getUser();
        if(user==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"非法进入");
        }

        Integer userId = user.getId();
        if(userId==0){
            //游客身份
            return ResponseResult.errorResult(AppHttpCodeEnum.NO_OPERATOR_AUTH,"当前用户是游客身份,无权操作");
        }

        //4.定义当前初始化状态
        Boolean islike=false,isunlike=false,iscollection=false,isfollow=false;

        //3.如果是用户登录的话，则查询是否点赞过，是否关注过，是否收藏过，是否不喜欢过
        //点赞
        String likeKey="USER_LIKES_"+dto.getArticleId();
        boolean likeFlag = redisCacheService.hExists(likeKey, userId.toString());
        islike=likeFlag;
        //关注
        String followKey="USER_FOLLOW_"+userId;
        boolean followFlag = redisCacheService.hExists(followKey, dto.getAuthorId().toString());
        isfollow=followFlag;

        //收藏
        String collectionKey="USER_COLLECTION_"+dto.getArticleId();
        boolean collectionFlag = redisCacheService.hExists(collectionKey, userId.toString());
        iscollection=collectionFlag;

        //不喜欢
        String unlikeKey="USER_UNLIKES_"+dto.getArticleId();
        boolean unlikeFlag = redisCacheService.hExists(unlikeKey, userId.toString());
        isunlike=unlikeFlag;

        //4.返回数据
        Map map=new HashMap();
        map.put("islike",islike);
        map.put("isunlike",isunlike);
        map.put("iscollection",iscollection);
        map.put("isfollow",isfollow);

        return ResponseResult.okResult(map);
    }

















}
