package com.heima.wemedia.service;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmNews;

import java.util.List;

public interface WmAutoScanService {
    /**
     * 审核自媒体文章
     */
    ResponseResult auditWmNews(WmNews wmNews, List<String> contentImageList, List<String> coverImageList);
}
