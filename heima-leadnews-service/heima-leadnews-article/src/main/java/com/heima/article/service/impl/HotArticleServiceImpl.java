package com.heima.article.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.heima.api.feign.WmChannelFeginClient;
import com.heima.article.mapper.ArticleMapper;
import com.heima.article.service.HotArticleService;
import com.heima.common.redis.RedisCacheService;
import com.heima.model.article.dtos.HotArticleVo;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.common.constants.ArticleConstants;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.message.ArticleVisitStreamMess;
import com.heima.model.wemedia.pojos.WmChannel;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class HotArticleServiceImpl implements HotArticleService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private WmChannelFeginClient wmChannelFeginClient;

    @Autowired
    private RedisCacheService redisCacheService;


    /**
     * 定时任务计算热点文章，没有返回值，也没有参数
     * 主方法
     */
    @Override
   // @XxlJob(value = "computeHotArticle")
    public void computeHotArticleToRedis() {
        //1.查询前五天的文章列表
        List<ApArticle> apArticleList = getApArticleListForLast5Days();
        //2.计算文章的分值
        List<HotArticleVo> hotArticleVoList = jisuanScore(apArticleList);
        //3.根据频道进行缓存数据到redis,分值最高的前三十条存储到redis中
        cacheArticleToRedis(hotArticleVoList);
    }



    private void cacheArticleToRedis(List<HotArticleVo> hotArticleVoList) {
        //3.1 获取所有的频道
        ResponseResult<List<WmChannel>> responseResult = wmChannelFeginClient.list();
        if(responseResult.getCode()!=200){
            return;
        }

        List<WmChannel> wmChannelList = responseResult.getData();
        //3.2 查询每个频道的文章，从hotArticleVoList中查询
        for (WmChannel wmChannel : wmChannelList) {
            //获取每一个频道下的文章列表
            List<HotArticleVo> hotArticleVos = hotArticleVoList.stream().filter(hotArticleVo -> wmChannel.getId().equals(hotArticleVo.getChannelId())).collect(Collectors.toList());
            cacheToRedis(hotArticleVos, ArticleConstants.HOT_ARTICLE_FIRST_PAGE + wmChannel.getId());
        }

        //4.根据推荐进行缓存数据到redis,分值最高的前三十条存储到redis中
        //4.1 按照分值排序
        cacheToRedis(hotArticleVoList, ArticleConstants.HOT_ARTICLE_FIRST_PAGE+ArticleConstants.DEFAULT_TAG);
    }

    /**
     * 存储数据到redis
     * @param hotArticleVoList
     * @param key
     */
    private void cacheToRedis(List<HotArticleVo> hotArticleVoList, String key) {
        //3.3 按照分值排序
        hotArticleVoList = hotArticleVoList.stream().sorted(Comparator.comparing(HotArticleVo::getScore).reversed()).collect(Collectors.toList());
        //3.4 取出前三十条分值较高的文章
        hotArticleVoList = hotArticleVoList.stream().limit(30).collect(Collectors.toList());
        //3.5 存入到redis中
        redisCacheService.set(key, JSON.toJSONString(hotArticleVoList));
    }

    /**
     * 抽取的方法，计算每一篇文章的分值
     * @param apArticleList
     * @return
     */
    private List<HotArticleVo> jisuanScore(List<ApArticle> apArticleList) {
        if (CollectionUtils.isEmpty(apArticleList)) {
            return null;
        }
        List<HotArticleVo> hotArticleVoList = apArticleList.stream().map(apArticle -> {
            HotArticleVo vo = new HotArticleVo();
            BeanUtils.copyProperties(apArticle, vo);
            //TODO 根据权重规则，计算出每一篇文章的分值
            Integer score=this.computeArticleByScore(apArticle);
            vo.setScore(score);
            return vo;
        }).collect(Collectors.toList());
        return hotArticleVoList;
    }

    /**
     * 抽取的方法，获取前五天的文章列表
     * @return
     */
    private List<ApArticle> getApArticleListForLast5Days() {
        Calendar calendar=Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH,-5);
        Date last5DayTime=calendar.getTime();
        List<ApArticle> apArticleList = articleMapper.loadArticleListForLast5Days(last5DayTime);
        return apArticleList;
    }

    /**
     * 计算每一篇文章的分值
     * 阅读权重：1
     * 点赞权重：3
     * 评论权重：5
     * 收藏权重：8
     * @param apArticle
     * @return
     */
    private Integer computeArticleByScore(ApArticle apArticle) {
        Integer score=0;
        //阅读
        if (apArticle.getViews()!=null) {
            score+=apArticle.getViews();
        }
        //点赞
        if (apArticle.getLikes()!=null) {
            score+=apArticle.getLikes()* ArticleConstants.HOT_ARTICLE_LIKE_WEIGHT;
        }
        //评论
        if (apArticle.getComment()!=null) {
            score+=apArticle.getComment()*ArticleConstants.HOT_ARTICLE_COMMENT_WEIGHT;
        }
        //收藏
        if (apArticle.getCollection()!=null) {
            score+=apArticle.getCollection()*ArticleConstants.HOT_ARTICLE_COLLECTION_WEIGHT;
        }
        return score;
    }


    /**
     * 实时计算，更新文章分值的方法
     *
     * @param mess
     */
    @Override
    public void updateScore(ArticleVisitStreamMess mess) {

        //1.根据文章id,查询ap_article
        ApArticle apArticle = articleMapper.selectById(mess.getArticleId());
        if(apArticle==null){
            return;
        }
        //2.修改行为数量
        apArticle.setLikes(apArticle.getLikes()+mess.getLike());
        apArticle.setCollection(apArticle.getCollection()+mess.getCollect());
        apArticle.setViews(apArticle.getViews()+mess.getView());
        apArticle.setComment(apArticle.getComment()+mess.getComment());
        articleMapper.updateById(apArticle);

        //3.重新计算文章分值score
        Integer score = this.computeArticleByScore(apArticle);
        score= score*3;

        //4.替换redis中的数据
        //4.1 按照频道
        replaceDataToRedis(apArticle, score,ArticleConstants.HOT_ARTICLE_FIRST_PAGE + apArticle.getChannelId());

        //4.2 按照推荐
        replaceDataToRedis(apArticle, score,ArticleConstants.HOT_ARTICLE_FIRST_PAGE +ArticleConstants.DEFAULT_TAG);
    }

    /**
     * 抽取的替换redis方法
     * @param apArticle
     * @param score
     * @param
     */
    private void replaceDataToRedis(ApArticle apArticle, Integer score,String key) {
        //声明一个标识
        Boolean flag=true;

        String hotArticleVoListStr = redisCacheService.get(key);
        if (StringUtils.isNotEmpty(hotArticleVoListStr)) {
            List<HotArticleVo> hotArticleVos = JSONArray.parseArray(hotArticleVoListStr, HotArticleVo.class);
            for (HotArticleVo hotArticleVo : hotArticleVos) {
                //表示当前缓存中有这篇文章
                if (hotArticleVo.getId().equals(apArticle.getId())) {
                    //更新分值
                    hotArticleVo.setScore(score);
                    flag =false;
                    break;
                }
            }
            //缓存中不存在的情况
            if (flag) {
                //缓存中的长度大于30
                if (hotArticleVos.size()>=30) {
                    //1.倒序排序
                    hotArticleVos=hotArticleVos.stream().sorted(Comparator.comparing(HotArticleVo::getScore).reversed()).collect(Collectors.toList());
                    //2.取出最后一个元素
                    HotArticleVo lastHotArticleVo = hotArticleVos.get(hotArticleVos.size() - 1);
                    //3.比较大小
                    if (lastHotArticleVo.getScore()< score) {
                        HotArticleVo hotArticleVo=new HotArticleVo();
                        BeanUtils.copyProperties(apArticle,hotArticleVo);
                        hotArticleVo.setScore(score);
                        hotArticleVos.add(hotArticleVo);
                    }
                }else{
                    //小于30
                    HotArticleVo hotArticleVo=new HotArticleVo();
                    BeanUtils.copyProperties(apArticle,hotArticleVo);
                    hotArticleVo.setScore(score);
                    hotArticleVos.add(hotArticleVo);
                }
            }
            //重新写入到redis中
            hotArticleVos=hotArticleVos.stream().sorted(Comparator.comparing(HotArticleVo::getScore).reversed()).collect(Collectors.toList());
            redisCacheService.set(key,JSON.toJSONString(hotArticleVos));
        }
    }
}
