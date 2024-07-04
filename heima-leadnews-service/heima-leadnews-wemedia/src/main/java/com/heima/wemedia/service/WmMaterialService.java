package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.WmMaterialDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import org.springframework.web.multipart.MultipartFile;

public interface WmMaterialService extends IService<WmMaterial> {

    /**
     * 素材上传
     * @return
     */
    ResponseResult upload(MultipartFile file);

    /**
     * 查询素材列表
     * @param dto
     * @return
     */
    ResponseResult list(WmMaterialDto dto);
    /**
     * 删除素材
     * @param id
     * @return
     */
    ResponseResult del(Integer id);
    /**
     * 取消收藏
     * @param id
     * @return
     */
    ResponseResult cancel(Integer id);
    /**
     * 收藏
     * @param id
     * @return
     */
    ResponseResult collect(Integer id);
}
