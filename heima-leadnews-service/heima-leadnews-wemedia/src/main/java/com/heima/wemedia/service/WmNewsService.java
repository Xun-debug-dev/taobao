package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmNews;

public interface WmNewsService extends IService<WmNews> {


    /**
     * 查询文章列表
     * @param dto
     * @return
     */
    ResponseResult list(WmNewsPageReqDto dto);

    /**
     * 保存-修改-提交草稿为一体的方法
     * @param dto
     * @return
     */
    ResponseResult submit(WmNewsDto dto);
    /**
     * 文章上下架
     * @param dto
     * @return
     */
    ResponseResult downOrUp(WmNewsDto dto);
}
