package com.heima.article.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.article.mapper.ArticleConfigMapper;
import com.heima.article.mapper.ArticleContentMapper;
import com.heima.article.mapper.ArticleMapper;
import com.heima.article.service.ApArticleConfigService;
import com.heima.article.service.ApArticleContentService;
import com.heima.article.service.ApArticleService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleConfig;
import com.heima.model.article.pojos.ApArticleContent;
import com.heima.model.common.constants.ArticleConstants;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.search.dtos.SearchArticleVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;


@Service
public class ApApArticleServiceImpl extends ServiceImpl<ArticleMapper, ApArticle> implements ApArticleService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private ArticleContentMapper articleContentMapper;

    @Autowired
    private ArticleConfigMapper articleConfigMapper;



    /**
     * 加载首页-加载更多-加载更新 三位一体
     *
     * @param dto  type=1 认为是加载更多，type=2表示加载更新
     * @param type
     * @return
     */
    @Override
    public ResponseResult load(ArticleHomeDto dto, Short type) {
        //1.非空判断
        if(dto.getSize()==0){
            dto.setSize(10);
        }
        if(dto.getTag()==null){
            dto.setTag(ArticleConstants.DEFAULT_TAG);
        }
        if(dto.getMinBehotTime()==null){
            dto.setMinBehotTime(new Date());
        }
        if(dto.getMaxBehotTime()==null){
            dto.setMaxBehotTime(new Date());
        }
        //2. 直接调用sql,执行查询(判断类型，排序)
        List<ApArticle> apArticles = articleMapper.loadArticleList(dto, type);

        //3.返回数据
        return ResponseResult.okResult(apArticles);
    }

    @Autowired
    private ApArticleConfigService apArticleConfigService;

    @Autowired
    private ApArticleContentService apArticleContentService;

    @Autowired
    private RabbitTemplate rabbitTemplate;


    /**
     * 保存三剑客
     *
     * @param dto
     * @return
     */
    @Override
    @Transactional
    public ResponseResult save(ArticleDto dto) {
        //1.判断参数是否为空
        if(StringUtils.isBlank(dto.getTitle()) || StringUtils.isBlank(dto.getContent())){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE);
        }

        ApArticle apArticle = new ApArticle();
        BeanUtils.copyProperties(dto, apArticle);

        //2.id无值就创建文章
        if(apArticle.getId()==null){
            //保存文章主表数据
            this.save(apArticle);

            //保存文章配置表数据
            ApArticleConfig config = new ApArticleConfig();
            config.setArticleId(apArticle.getId());
            config.setIsComment(true); //允许评论
            config.setIsForward(true); //允许转发
            config.setIsDelete(false); //未删除
            config.setIsDown(false); //未下架
            apArticleConfigService.save(config);

            //保存文章内容表数据
            ApArticleContent content = new ApArticleContent();
            content.setArticleId(apArticle.getId());
            content.setContent(dto.getContent());
            apArticleContentService.save(content);
        } else {
            //3.id有值更新文章

            //判断文章是否存在  select count(*) from ap_article where id=?
            Integer countArticle = this.lambdaQuery().eq(ApArticle::getId, apArticle.getId()).count();
            if(countArticle<=0){
                return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "文章不存在无法更新");
            }

            //更新文章主表
            this.updateById(apArticle);

            //判断文章内容是否存在  select count(*) from ap_article_content where article_id=?
            Integer countContent = apArticleContentService.lambdaQuery().eq(ApArticleContent::getArticleId, apArticle.getId()).count();
            if(countContent<=0){
                return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "文章内容不存在无法更新");
            }

            //更新文章内容表  update ap_article_content set content=? where article_id=?
            apArticleContentService.update(Wrappers.<ApArticleContent>lambdaUpdate()
                    .set(ApArticleContent::getContent, dto.getContent())   //更新的字段和值
                    .eq(ApArticleContent::getArticleId, apArticle.getId())  //更新条件
            );
        }

        //4.生产文章消息到RabbitMQ的队列中
        SearchArticleVo vo = new SearchArticleVo();
        BeanUtils.copyProperties(apArticle,vo);
        vo.setLayout(Integer.valueOf(String.valueOf(apArticle.getLayout())));
        vo.setContent(dto.getContent());
        rabbitTemplate.convertAndSend(ArticleConstants.ARTICLE_ES_SYNC_TOPIC,ArticleConstants.ARTICLE_ES_SYNC_ROUTINGKEY, JSON.toJSONString(vo));
        //5.响应文章主表ID
        return ResponseResult.okResult(apArticle.getId());
    }

}
