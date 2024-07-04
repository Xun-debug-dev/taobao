package com.heima.comment.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.heima.api.feign.ApUserFeignClient;
import com.heima.comment.pojos.ApComment;
import com.heima.comment.pojos.ApCommentLike;
import com.heima.comment.pojos.ApCommentVo;
import com.heima.comment.service.CommentRepayService;
import com.heima.common.redis.RedisCacheService;
import com.heima.model.comment.dtos.CommentRepayDto;
import com.heima.model.comment.dtos.CommentRepayLikeDto;
import com.heima.model.comment.dtos.CommentRepayListDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.pojo.ApUser;
import com.heima.utils.threadlocal.AppThreadLocalUtil;
import org.bson.types.ObjectId;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class CommentRepayServiceImpl implements CommentRepayService {

    @Autowired
    private RedisCacheService redisCacheService;

    @Autowired
    private ApUserFeignClient apUserFeignClient;

    @Autowired
    private MongoTemplate mongoTemplate;
    /**
     * 发表评论回复--针对评论
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult saveComment(CommentRepayDto dto) {
        //1.非空判断
        if (dto.getCommentId()==null || dto.getContent()==null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        if (dto.getContent().length()>140) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"评论内容长度超过140个字");
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

        //2.审核评论内容,TODO

        //3.先根据key去查询redis中的用户信息，如果有，则直接返回
        String key="LOGIN_USER_"+userId;
        String userStr = redisCacheService.get(key);
        ApUser apUser=null;
        if(userStr!=null){
            apUser= JSONObject.parseObject(userStr, ApUser.class);
        }else{
            //4.如果没有，则远程调用用户微服务，获取用户信息，并同步redis
            ResponseResult responseResult = apUserFeignClient.findUserById(userId);
            if(responseResult.getCode()==200 && responseResult.getData()!=null){
                apUser = (ApUser) responseResult.getData();
                //同步redis
                redisCacheService.setEx(key, JSON.toJSONString(apUser),2L, TimeUnit.HOURS);
            }
        }
        //5.新增mongo
        ApComment apComment=new ApComment();
        apComment.setId(ObjectId.get().toString());
        apComment.setUserId(userId);
        apComment.setNickName(apUser.getName());
        apComment.setImage(apUser.getImage());
        apComment.setTargetId(dto.getCommentId());
        apComment.setContent(dto.getContent());
        apComment.setLikes(0);
        apComment.setReply(0);
        apComment.setCreatedTime(new Date());
        apComment.setUpdatedTime(new Date());
        mongoTemplate.save(apComment);

        //根据条件对apcomment表中的reply回复字段+1
        Query query=Query.query(Criteria.where("id").is(dto.getCommentId()));
        UpdateDefinition update=new Update().inc("reply",1);
        mongoTemplate.updateFirst(query,update,ApComment.class);

        //6.返回数据
        return ResponseResult.okResult("发表评论完成");
    }

    /**
     * 点赞评论回复动作
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult likeComment(CommentRepayLikeDto dto) {
        //1.非空判断
        if (dto.getCommentRepayId()==null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        //2.是否游客身份
        ApUser user = AppThreadLocalUtil.getUser();
        if(user==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"非法进入");
        }

        Integer userId = user.getId();
        if(userId==0){
            //游客身份
            return ResponseResult.errorResult(AppHttpCodeEnum.NO_OPERATOR_AUTH,"当前用户是游客身份,无权操作");
        }

        //2.5 根据评论id查询评论表是否存在
        ApComment apComment = mongoTemplate.findOne(Query.query(Criteria.where("id").is(dto.getCommentRepayId())), ApComment.class);
        if (apComment==null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"评论信息不存在");
        }

        //3.查询redis中是否点赞过该评论
        String key="COMMENT_REPAY_LIKE_"+userId;
        Boolean flag = redisCacheService.sIsMember(key, dto.getCommentRepayId());


        //3.1.如果点赞过，且正在执行的是点赞操作，则直接返回报错信息
        if(flag && dto.getOperation()==0){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_EXIST,"当前用户已经点赞过该评论");
        }

        //3.2.如果点赞过，且正在执行的是取消点赞操作，则删除redis中的信息，修改评论表中的点赞数据-1，删除评论点赞表信息
        if (flag && dto.getOperation()==1) {
            //删除redis
            redisCacheService.sRemove(key,dto.getCommentRepayId());
            //修改apcomment表 点赞数量-1
            /*apComment.setLikes(apComment.getLikes()>0?apComment.getLikes()-1:0);
            mongoTemplate.save(apComment);*/

            Query query=Query.query(Criteria.where("id").is(dto.getCommentRepayId()).and("likes").gt(0));
            UpdateDefinition update=new Update().inc("likes",-1);
            mongoTemplate.updateFirst(query,update,ApComment.class);

            //删除apcommentlike表
            mongoTemplate.remove(Query.query(
                    Criteria.where("userId").is(userId).and("targetId").is(dto.getCommentRepayId())
            ), ApCommentLike.class);
        }

        //3.3 如果没有点赞过，且正在执行的是点赞操作，则新增redis中的信息，以及修改评论表中的点赞数据+1，新增评论点赞表的数据
        if(!flag && dto.getOperation()==0){
            //新增redis
            redisCacheService.sAdd(key,dto.getCommentRepayId());
            //修改评论表
            Query query=Query.query(Criteria.where("id").is(dto.getCommentRepayId()));
            UpdateDefinition update=new Update().inc("likes",1);
            mongoTemplate.updateFirst(query,update,ApComment.class);
            //新增评论点赞表
            ApCommentLike apCommentLike=new ApCommentLike();
            apCommentLike.setId(ObjectId.get().toString());
            apCommentLike.setUserId(userId);
            apCommentLike.setTargetId(dto.getCommentRepayId());
            apCommentLike.setCreatedTime(new Date());
            mongoTemplate.insert(apCommentLike);
        }

        //4.返回数据
        apComment = mongoTemplate.findOne(Query.query(Criteria.where("id").is(dto.getCommentRepayId())), ApComment.class);
        Map map=new HashMap();
        map.put("likes",apComment.getLikes());

        return ResponseResult.okResult(map);
    }

    /**
     * 查询评论回复列表
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult loadComment(CommentRepayListDto dto) {
        //1.非空判断
        if (dto.getCommentId()==null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"文章id不存在");
        }

        if (dto.getSize()==null) {
            dto.setSize(10);
        }

        //2.根据文章id查询mongodb,时间倒序展示，分页查询默认10条数据，返回list
        Query query=Query.query(
                Criteria.where("targetId").is(dto.getCommentId())
                        .and("createdTime").lte(dto.getMinDate())
        ).with(Sort.by(Sort.Order.desc("createdTime"))).limit(dto.getSize());
        List<ApComment> apCommentList = mongoTemplate.find(query, ApComment.class); //list中有两条数据  id=007 id=001


        //3.如果是游客身份，直接返回list
        ApUser user = AppThreadLocalUtil.getUser();
        if(user==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"非法进入");
        }
        Integer userId = user.getId();
        if(userId==0){
            return ResponseResult.okResult(apCommentList);
        }

        //4.如果是登录用户身份，我们是要去查询当前用户是否点赞该文章，如果点赞过，则加入标识，并返回
        String key="COMMENT_REPAY_LIKE_"+userId;
        Set<String> commentIdSet = redisCacheService.setMembers(key);

        //两个集合取交集，apCommentList和commentIdSet
        List<ApCommentVo> apCommentVos = apCommentList.stream().map(apComment -> {
            //实例化vo
            ApCommentVo vo = new ApCommentVo();
            BeanUtils.copyProperties(apComment, vo);
            //说明有交集，点赞过
            if (commentIdSet.contains(apComment.getId())) {
                vo.setOperation((short) 0);
            }
            return vo;
        }).collect(Collectors.toList());

        return ResponseResult.okResult(apCommentVos);
    }
}
