package com.heima.wemedia.service.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.heima.api.feign.ApArticleFeignClient;
import com.heima.audit.baidu.BaiduImageScan;
import com.heima.audit.baidu.BaiduTextScan;
import com.heima.audit.tess4j.Tess4jClient;
import com.heima.common.constants.WmNewsConstants;
import com.heima.common.exception.CustomException;
import com.heima.common.redis.RedisCacheService;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.model.wemedia.pojos.WmSensitive;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.utils.common.SensitiveWordUtil;
import com.heima.wemedia.service.*;
import net.sourceforge.tess4j.TesseractException;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WmAutoScanServiceImpl implements WmAutoScanService {


    @Autowired
    private BaiduTextScan baiduTextScan;

    @Autowired
    private BaiduImageScan baiduImageScan;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ApArticleFeignClient apArticleFeignClient;

    @Autowired
    private WmChannelService wmChannelService;

    @Autowired
    private WmUserService wmUserService;

    @Autowired
    private WmNewsService wmNewsService;

    @Autowired
    private WmNewsMaterialService wmNewsMaterialService;

    @Autowired
    private WmMaterialService wmMaterialService;

    @Autowired
    private Tess4jClient tess4jClient;

    /**
     * 文章自动审核
     *
     * @param wmNews
     */
    @Async("taskExecutor")
    public ResponseResult auditWmNews(WmNews wmNews, List<String> contentImageList, List<String> coverImageList) {

//        try {
//            TimeUnit.SECONDS.sleep(10); //模拟审核耗时超过10秒
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        //1.准备待审核文本
        String allText = prepareAllText(wmNews);

        //2.准备待审核图片(一定要去重图片)
        List<String> allImage = this.prepareAllImage(contentImageList, coverImageList);


        //提取所有图片中的文本
        String ocrText = this.ocrImageText(allImage);

        allText = allText + ocrText;

        //3.DFA算法文本审核
        ResponseResult responseResult = this.dfaTextScan(wmNews, allText);
        if (responseResult != null) {
            return responseResult;
        }

        //4.百度云文本审核
        responseResult = this.baiduTextScan(wmNews, allText);
        if (responseResult != null) {
            return responseResult;
        }

        //5.百度云图片审核
        responseResult = this.baiduImageScan(wmNews, allImage);
        if (responseResult != null) {
            return responseResult;
        }

        //6.发布时间未到，修改文章状态为审核通过待发布
        long publishTime = wmNews.getPublishTime().getTime();//文章发布时间
        long currentTime = DateTime.now().getMillis(); //系统当前时间
        if (publishTime > currentTime) {

            //TODO 生成文章消息到Rabbit的死信队列中

            this.updateWmNews(wmNews, WmNews.Status.SUCCESS.getCode(), "审核通过待发布");
        } else {

            //7.发布时间已到，创建APP端文章且修改文章状态为已发布
            this.doArticlePublish(wmNews);
        }
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * OCR技术提取所有图片中的文本
     * @param allImage
     * @return
     */
    private String ocrImageText(List<String> allImage){
        StringBuffer ocrBuffer = new StringBuffer();
        if (allImage.size() > 0) {
            for (String url : allImage) {

                byte[] bytes = fileStorageService.downLoadFile(url);

                try {
                    //从byte[]转换为butteredImage
                    ByteArrayInputStream in = new ByteArrayInputStream(bytes);
                    BufferedImage imageFile = ImageIO.read(in);
                    //识别图片的文字
                    String result = tess4jClient.doOCR(imageFile);
                    if(StringUtils.isNotBlank(result)){
                        ocrBuffer.append(result);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (TesseractException e) {
                    e.printStackTrace();
                }
            }
        }
        return ocrBuffer.toString();
    }

    @Autowired
    private WmSensitiveService wmSensitiveService;

    @Autowired
    private RedisCacheService redisCacheService;

    private static final String SENSITIVE_KEY = "sensitive:list";

    /**
     * DFA算法本地审核
     *
     * @param wmNews
     * @param allText
     * @return
     */
    private ResponseResult dfaTextScan(WmNews wmNews, String allText) {

        //1. 从Redis缓存中查询敏感词列表
        List<String> sensitiveList = redisCacheService.lRange(SENSITIVE_KEY, 0, -1);
        if(sensitiveList.isEmpty()){
            //2. 如果缓存没数据就从敏感词表中查询所有敏感词，再更新到Redis缓存中  select sensitives from wm_sensitive
            List<WmSensitive> wmSensitiveList = wmSensitiveService.lambdaQuery().select(WmSensitive::getSensitives).list();
            sensitiveList = wmSensitiveList.stream().map(WmSensitive::getSensitives).collect(Collectors.toList());

            redisCacheService.lLeftPushAll(SENSITIVE_KEY,sensitiveList);
        }

        //2.初始化DFA敏感词库
        SensitiveWordUtil.initMap(sensitiveList);

        //3.进行文本审核
        Map<String, Integer> result = SensitiveWordUtil.matchWords(allText);

        //4.根据审核结果处理
        if (result.size() > 0) {
            this.updateWmNews(wmNews, WmNews.Status.FAIL.getCode(), "DFA文本审核违规");
            return ResponseResult.errorResult(AppHttpCodeEnum.SERVER_ERROR, "DFA文本审核违规");
        }

        return null;

    }

    /**
     * 执行文章发布
     *
     * @param wmNews
     */
    private void doArticlePublish(WmNews wmNews) {
        //6.1 准备dto参数数据
        ArticleDto dto = new ArticleDto();
        dto.setId(wmNews.getArticleId());//app端文章id
        dto.setTitle(wmNews.getTitle());//文章标题
        dto.setLabels(wmNews.getLabels());//文章标签
        dto.setContent(wmNews.getContent());//文章内容
        dto.setLayout(wmNews.getType());//布局方式
        dto.setImages(wmNews.getImages());//封面图片地址
        dto.setPublishTime(wmNews.getPublishTime());//文章发布时间
        if (dto.getId() == null) {
            dto.setCreatedTime(new Date());//创建时间
        }
        //查询频道
        WmChannel wmChannel = wmChannelService.getById(wmNews.getChannelId());
        if (wmChannel != null) {
            dto.setChannelId(wmChannel.getId());//频道id
            dto.setChannelName(wmChannel.getName());//频道名称
        }
        //查询自媒体用户
        WmUser wmUser = wmUserService.getById(wmNews.getUserId());
        if (wmUser != null) {
            dto.setAuthorId(wmUser.getApAuthorId().longValue()); //作者ID
            dto.setAuthorName(wmUser.getName());//作者名
        }
        //设置行为数量缺省值为0
        dto.setViews(0); //阅读数量
        dto.setLikes(0); //点赞数量
        dto.setComment(0);//评论数量
        dto.setCollection(0);//收藏数量

        //6.2 调用feign接口创建或更新app端文章
        ResponseResult responseResult = apArticleFeignClient.save(dto);
        if (responseResult.getCode() != 200) {
            throw new CustomException(AppHttpCodeEnum.FEIGN_INVOKE_ERROR);
        }

        //6.3 得到响应结果里的app端文章id设置到自媒体文章中
        Long articleId = Long.valueOf(String.valueOf(responseResult.getData()));
        wmNews.setArticleId(articleId);

        //6.4 更新自媒体文章状态为已发布
        this.updateWmNews(wmNews, WmNews.Status.PUBLISHED.getCode(), "审核通过已发布");
    }

    /**
     * 百度云图片审核
     *
     * @param wmNews
     * @param allImage
     * @return
     */
    private ResponseResult baiduImageScan(WmNews wmNews, List<String> allImage) {
        if (allImage.size() > 0) {
            for (String imageUrl : allImage) {
                byte[] bytes = fileStorageService.downLoadFile(imageUrl);
                Integer imageResult = baiduImageScan.imageScan(bytes);
                if (imageResult == WmNewsConstants.BAIDU_FAIL) {

                    this.updateWmNews(wmNews, WmNews.Status.FAIL.getCode(), "百度云图片审核违规");
                    return ResponseResult.errorResult(AppHttpCodeEnum.SERVER_ERROR, "百度云图片审核违规");
                } else if (imageResult == WmNewsConstants.BAIDU_MAYBE) {

                    this.updateWmNews(wmNews, WmNews.Status.ADMIN_AUTH.getCode(), "百度云图片审核不确定");
                    return ResponseResult.errorResult(AppHttpCodeEnum.SERVER_ERROR, "百度云图片审核不确定");
                }
            }
        }
        return null;
    }

    /**
     * 百度云文本审核
     *
     * @param wmNews
     * @param allText
     * @return
     */
    private ResponseResult baiduTextScan(WmNews wmNews, String allText) {
        Integer textResult = baiduTextScan.textScan(allText);
        if (textResult == WmNewsConstants.BAIDU_FAIL) {

            this.updateWmNews(wmNews, WmNews.Status.FAIL.getCode(), "百度云文本审核违规");
            return ResponseResult.errorResult(AppHttpCodeEnum.SERVER_ERROR, "百度云文本审核违规");
        } else if (textResult == WmNewsConstants.BAIDU_MAYBE) {

            this.updateWmNews(wmNews, WmNews.Status.ADMIN_AUTH.getCode(), "百度云文本审核不确定");
            return ResponseResult.errorResult(AppHttpCodeEnum.SERVER_ERROR, "百度云文本审核不确定");
        }
        return null;
    }

    private List<String> prepareAllImage(List<String> contentImageList, List<String> coverImageList) {
        List<String> allImage = new ArrayList<>();
        allImage.addAll(contentImageList);
        allImage.addAll(coverImageList);
        if (allImage.size() > 0) {
            allImage = allImage.stream().distinct().collect(Collectors.toList());
        }
        return allImage;
    }

    /**
     * 公共方法：更新文章状态和原因
     *
     * @param wmNews
     * @param status
     * @param reason
     */
    private void updateWmNews(WmNews wmNews, short status, String reason) {
        wmNews.setStatus(status);//文章状态
        wmNews.setReason(reason);
        wmNewsService.updateById(wmNews);
    }

    private static String prepareAllText(WmNews wmNews) {
        StringBuffer allTextBuffer = new StringBuffer();
        allTextBuffer.append(wmNews.getTitle());// 文本来源：标题
        allTextBuffer.append(wmNews.getLabels()); //文本来源：标签

        List<Map> contentMap = JSON.parseArray(wmNews.getContent(), Map.class);

        if (contentMap != null && contentMap.size() > 0) {
            for (Map<String, String> map : contentMap) {
                String type = map.get("type");
                if ("text".equals(type)) {
                    String contentText = map.get("value");
                    allTextBuffer.append(contentText);
                }
            }
        }
        String allText = allTextBuffer.toString();
        return allText;
    }


}
