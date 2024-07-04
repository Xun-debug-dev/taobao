package com.heima.wemedia.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.common.constants.WmNewsConstants;
import com.heima.model.common.constants.WmNewsMessageConstants;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmNewsMaterial;
import com.heima.utils.threadlocal.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.service.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class WmNewsServiceImpl extends ServiceImpl<WmNewsMapper, WmNews> implements WmNewsService {

    @Autowired
    private WmNewsMaterialMapper wmNewsMaterialMapper;

    @Autowired
    private WmMaterialMapper wmMaterialMapper;

    @Autowired
    private WmNewsTaskService wmNewsTaskService;

    @Autowired
    private RabbitTemplate rabbitTemplate;


    /**
     * 查询文章列表
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult list(WmNewsPageReqDto dto) {
        //1.非空判断
        dto.checkParam();
        //2.分页查询
        IPage<WmNews> page=new Page<>(dto.getPage(),dto.getSize());
        LambdaQueryWrapper<WmNews> queryWrapper= Wrappers.lambdaQuery();
        //2.1 根据状态查询
        if(dto.getStatus()!=null){
            queryWrapper.eq(WmNews::getStatus,dto.getStatus());
        }

        //2.2 根据关键字查询
        if(!StringUtils.isEmpty(dto.getKeyword())){
            queryWrapper.like(WmNews::getTitle,dto.getKeyword());
        }

        //2.3 根据频道id查询
        if(dto.getChannelId()!=null){
            queryWrapper.eq(WmNews::getChannelId,dto.getChannelId());
        }

        //2.4 根据开始和结束时间查询
        if(dto.getBeginPubdate()!=null && dto.getEndPubdate()!=null){
            queryWrapper.between(WmNews::getPublishTime,dto.getBeginPubdate(),dto.getEndPubdate());
        }

        //2.5 根据登录用户id查询
        queryWrapper.eq(WmNews::getUserId, WmThreadLocalUtil.getUserId());

        page=super.page(page,queryWrapper);

        //3.返回数据结果
        ResponseResult result=new PageResponseResult(dto.getPage(),dto.getSize(), (int) page.getTotal());
        result.setData(page.getRecords());
        return result;
    }

    @Autowired
    private WmNewsMaterialService wmNewsMaterialService;

    @Autowired
    private WmMaterialService wmMaterialService;

    @Autowired
    private WmAutoScanService wmAutoScanService;

    /**
     * 保存-修改-提交草稿为一体的方法
     *  主方法
     * @param dto
     * @return
     */
    @Transactional
    @Override
    public ResponseResult submit(WmNewsDto dto) {
        Short isSubmit = dto.getStatus(); //0-保存草稿  1-提交审核
        //1. 判断参数是否为空
        if(StringUtils.isBlank(dto.getTitle()) || StringUtils.isBlank(dto.getContent())){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE);
        }

        //2. 处理公共逻辑-保存或更新文章
        //2.1 复制dto给pojo
        WmNews wmNews = new WmNews();
        BeanUtils.copyProperties(dto, wmNews);

        //2.2 单独处理images的值（将封面图片地址列表转为逗号拼接的字符串）
        List<String> coverImageList = dto.getImages();
        //String coverImageStr = String.join(",", coverImageList);
        //String coverImageStr = StringUtils.join(coverImageList, ",");
        if(coverImageList.size()>0){
            String coverImageStr = coverImageList.stream().collect(Collectors.joining(","));
            wmNews.setImages(coverImageStr);
        }

        //2.3 如果是自动布局则设置布局方式临时为空
        if(dto.getType()== WmNewsConstants.LAYOUT_AUTO){
            wmNews.setType(null);
        }

        //2.4 根据ID决定保存或更新文章
        ResponseResult responseResult = this.saveOrUpdateWmNews(wmNews);
        if(responseResult!=null){
            return responseResult;
        }

        //3. 保存关系数据-保存内容图片与文章关系
        List<String> contentImageList = this.extractContentImage(dto, isSubmit, wmNews);

        //4. 保存关系数据-保存封面图片与文章关系
        this.saveRelationForCover(dto, isSubmit, wmNews, coverImageList, contentImageList);
        return ResponseResult.okResult("发布文章完成");
    }

    /**
     * 保存或更新文章
     * @param wmNews
     * @return
     */
    private ResponseResult saveOrUpdateWmNews(WmNews wmNews) {
        //设置pojo属性的默认值
        wmNews.setSubmitedTime(new Date());
        wmNews.setUserId(WmThreadLocalUtil.getUserId());
        wmNews.setEnable(WmNewsConstants.ENALBE_UP);

        //如果ID无值，则保存文章
        if(wmNews.getId()==null){
            wmNews.setCreatedTime(new Date());
            this.save(wmNews);
        } else {
            //如果ID有值，则删除关系且更新文章

            //删除关系   delete from wm_news_material where news_id=?
            wmNewsMaterialService.remove(Wrappers.<WmNewsMaterial>lambdaQuery().eq(WmNewsMaterial::getNewsId, wmNews.getId()));

            //先判断文章是否存在  select count(*) from wm_news where id=?
            Integer count = this.lambdaQuery().eq(WmNews::getId, wmNews.getId()).count();
            if(count<=0){
                return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "文章不存在无法更新");
            }

            //再更新文章
            this.updateById(wmNews);
        }
        return null;
    }

    private void saveRelationForCover(WmNewsDto dto, Short isSubmit, WmNews wmNews, List<String> coverImageList, List<String> contentImageList) {
        if(isSubmit == WmNews.Status.SUBMIT.getCode()){
            //处理自动布局
            if(dto.getType()==WmNewsConstants.LAYOUT_AUTO){
                int size = contentImageList.size(); //内容图片数量
                //处理自动匹配规则：由内容图片数量来决定封面图片的多少及最终布局方式
                // 自动匹配规则1：内容图片数量>=3，则选3张图片当做封面图片，最终布局方式为多图布局
                if(size>=3){
                    coverImageList = contentImageList.stream().limit(3).collect(Collectors.toList());
                    wmNews.setType(WmNewsConstants.LAYOUT_MANY);
                } else if(size>=1 && size<3){
                    // 自动匹配规则2：内容图片数量>=1，则选1张图片当做封面图片，最终布局方式为单图布局
                    coverImageList = contentImageList.stream().limit(1).collect(Collectors.toList());
                    wmNews.setType(WmNewsConstants.LAYOUT_SINGLE);
                } else {
                    // 自动匹配规则3：内容图片数量==0，最终布局方式为无图布局
                    wmNews.setType(WmNewsConstants.LAYOUT_NONE);
                }
                //如果自动匹配规则匹配到了封面图片，则需要转为逗号拼接的字符串
                if(coverImageList.size()>0){
                   String coverImgStr =  coverImageList.stream().collect(Collectors.joining(","));
                   wmNews.setImages(coverImgStr);
                }
                //更新文章（主要是更新布局方式和封面图片地址）
                this.updateById(wmNews);
            }
            //保存封面图片与文章的关系
            if(coverImageList !=null && coverImageList.size()>0){
                this.saveRelation(wmNews.getId(), coverImageList, WmNewsConstants.REFRENCE_COVER);
            }
            //提交审核（文章自动审核） -- 提交异步任务到线程池中进行审核
            wmAutoScanService.auditWmNews(wmNews, contentImageList, coverImageList);
        }
    }

    /**
     * 抽取内容图片保存关系
     * @param dto
     * @param isSubmit
     * @param wmNews
     * @return
     */
    private List<String> extractContentImage(WmNewsDto dto, Short isSubmit, WmNews wmNews) {
        //3.1 抽取内容中所有图片的地址列表
        List<String> contentImageList = new ArrayList<>();
        List<Map> contentMapList = JSON.parseArray(dto.getContent(), Map.class);
        if(contentMapList!=null && contentMapList.size()>0 && isSubmit == WmNews.Status.SUBMIT.getCode()){
            for (Map<String,String> map : contentMapList) {
                String type = map.get("type");
                if("image".equals(type)){
                    String contentImageUrl = map.get("value");
                    contentImageList.add(contentImageUrl);
                }
            }

            //3.2 保存关系数据
            this.saveRelation(wmNews.getId(),contentImageList, WmNewsConstants.REFRENCE_CONTENT);
        }
        return contentImageList;
    }

    /**
     * 保存关系数据
     * @param wmNewsId  自媒体文章id
     * @param imageList  内容或封面地址列表
     * @param type  0-内容引用 1-主图引用
     */
    private void saveRelation(Integer wmNewsId, List<String> imageList, short type){
        //3.2 根据素材地址查询素材ID   select id from wm_material where url in (?,?,?);
        List<WmMaterial> wmMaterialList = wmMaterialService.lambdaQuery()
                .in(WmMaterial::getUrl, imageList).select(WmMaterial::getId).list();
        List<Integer> wmMaterialIdList = wmMaterialList.stream().map(WmMaterial::getId).collect(Collectors.toList());


        //3.3 构建关系数据
        short ord = 0;
        List<WmNewsMaterial> wmNewsMaterialList = new ArrayList<>();
        for (Integer materialId : wmMaterialIdList) {
            WmNewsMaterial wmNewsMaterial = new WmNewsMaterial();
            wmNewsMaterial.setNewsId(wmNewsId); // 自媒体文章id
            wmNewsMaterial.setMaterialId(materialId); // 素材ID（内容图片的ID）
            wmNewsMaterial.setType(type); //0-内容引用
            wmNewsMaterial.setOrd(ord++); //序号

            wmNewsMaterialList.add(wmNewsMaterial);
        }

        //3.4 保存关系数据  insert into  wm_news_material(news_id,material_id,type,ord)  values (?,?,?,?),(?,?,?,?),(?,?,?,?)
        wmNewsMaterialService.saveBatch(wmNewsMaterialList);
    }




    /**
     * 文章上下架
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult downOrUp(WmNewsDto dto) {

        //1. 判断文章是否存在
        WmNews wmNews = this.getById(dto.getId());
        if(wmNews==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "文章不存在，不允许操作");
        }

        //2. 判断文章状态是否已发布
        if(wmNews.getStatus()!=WmNews.Status.PUBLISHED.getCode()){
            return ResponseResult.errorResult(AppHttpCodeEnum.SERVER_ERROR, "文章未发布，不允许操作");
        }

        //3. 修改文章的enable值   update wm_news set enable=? where id=?
        WmNews wmNewsDB = new WmNews();
        wmNewsDB.setId(dto.getId());//更新条件
        wmNewsDB.setEnable(dto.getEnable());//更新的字段和值
        this.updateById(wmNewsDB);

        //4. 生产消息到rabbitmq的队列中
        Map msg = new HashMap();
        msg.put("articleId",wmNews.getArticleId());//App端文章id
        msg.put("enable",dto.getEnable());//Wedia文章上下架状态

        rabbitTemplate.convertAndSend(WmNewsMessageConstants.WM_NEWS_UP_OR_DOWN_TOPIC,
                WmNewsMessageConstants.WM_NEWS_UP_OR_DOWN_ROUTINGKEY, JSON.toJSONString(msg));

        return ResponseResult.okResult("文章上下架完成");
    }
}
