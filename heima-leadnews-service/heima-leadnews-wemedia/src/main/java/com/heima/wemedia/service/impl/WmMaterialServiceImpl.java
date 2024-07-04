package com.heima.wemedia.service.impl;

import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.file.service.FileStorageService;
import com.heima.model.common.dtos.PageResponseResult;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.WmMaterialDto;
import com.heima.model.wemedia.pojos.WmMaterial;
import com.heima.model.wemedia.pojos.WmNewsMaterial;
import com.heima.utils.threadlocal.WmThreadLocalUtil;
import com.heima.wemedia.mapper.WmMaterialMapper;
import com.heima.wemedia.mapper.WmNewsMaterialMapper;
import com.heima.wemedia.service.WmMaterialService;
import com.heima.wemedia.service.WmNewsMaterialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class WmMaterialServiceImpl extends ServiceImpl<WmMaterialMapper, WmMaterial> implements WmMaterialService {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private WmNewsMaterialMapper wmNewsMaterialMapper;

    /**文件大小上限*/
    private static final Integer maxSize =  2 * 1024 * 1024;

    /**常用图片类型*/
    private static final List<String> imageList = Arrays.asList("png", "jpg", "jpeg", "gif", "bmp");

    /**
     * 素材上传
     *
     * @param file
     * @return
     */
    @Override
    @Transactional
    public ResponseResult upload(MultipartFile file) {
        //1. 判断文件参数的合法性
        //1.1 判断文件是否为空
        if(file.isEmpty() || file.getSize()==0){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "文件不能为空");
        }

        //1.2 判断文件是否超限
        if(file.getSize()>maxSize){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "文件应小于2M");
        }

        try {
            //1.3 判断文件是否是图片类型
            String type = FileTypeUtil.getType(file.getInputStream());
            if(!imageList.contains(type)){
                return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "文件必须是图片类型");
            }

            //2. 生成唯一的文件名
            String fileName = IdUtil.fastSimpleUUID() + "." + type;

            //3. 上传文件到MinIO得到URL
            String url = fileStorageService.uploadImgFile("", fileName, file.getInputStream());

            //4. 保存素材到素材表中
            WmMaterial wmMaterial = new WmMaterial();
            wmMaterial.setUserId(WmThreadLocalUtil.getUserId());
            wmMaterial.setIsCollection((short)0);//未收藏
            wmMaterial.setType((short)0);//图片类型
            wmMaterial.setUrl(url);//图片地址
            wmMaterial.setCreatedTime(new Date());
            this.save(wmMaterial);

            return ResponseResult.okResult(wmMaterial);
        } catch (IOException e) {
            e.printStackTrace();

            return ResponseResult.errorResult(AppHttpCodeEnum.SERVER_ERROR, "上传失败");
        }
    }

    /**
     * 查询素材列表
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult list(WmMaterialDto dto) {
        //1.设置分页参数默认值
        dto.checkParam();

        //2.拼接查询条件
        LambdaQueryWrapper<WmMaterial> lambdaQueryWrapper = new LambdaQueryWrapper<>();

        //2.1 固定条件：根据用户ID查询
        lambdaQueryWrapper.eq(WmMaterial::getUserId, WmThreadLocalUtil.getUserId());

        //2.2 动态条件：根据已收藏查询
        lambdaQueryWrapper.eq(dto.getIsCollection()>0, WmMaterial::getIsCollection, dto.getIsCollection());

        //2.3 结果排序：根据创建时间倒排序
        lambdaQueryWrapper.orderByDesc(WmMaterial::getCreatedTime);

        //3.执行分页查询
        IPage<WmMaterial> page = new Page<>(dto.getPage(), dto.getSize());
        this.page(page, lambdaQueryWrapper);

        //4.封装分页响应结果
        ResponseResult responseResult = new PageResponseResult(dto.getPage(), dto.getSize(), (int)page.getTotal());
        responseResult.setData(page.getRecords());

        return responseResult;
    }

    @Autowired
    private WmNewsMaterialService wmNewsMaterialService;

    /**
     * 删除素材
     *
     * @param id
     * @return
     */
    @Override
    public ResponseResult del(Integer id) {
        //1. 判断数据是否空
        WmMaterial wmMaterial = this.getById(id);
        if(wmMaterial==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        //2. 判断素材是否被引用  select count(*) from wm_news_material where material_id=?
        Integer count = wmNewsMaterialService.lambdaQuery().eq(WmNewsMaterial::getMaterialId, id).count();
        if(count>0){
            return ResponseResult.errorResult(AppHttpCodeEnum.MATERIAL_REFRENCE_ERROR);
        }

        //3. 删除表中素材
        this.removeById(id);

        //4. 删除MinIO中素材
        fileStorageService.delete(wmMaterial.getUrl());

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 取消收藏
     *
     * @param id
     * @return
     */
    @Override
    public ResponseResult cancel(Integer id) {
        //1. 判断数据是否空
        WmMaterial wmMaterial = this.getById(id);
        if(wmMaterial==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        //2. 构建一个更新对象  update wm_material set is_collection=? where id=?
        WmMaterial wmMaterialDB = new WmMaterial();
        wmMaterialDB.setId(id);
        wmMaterialDB.setIsCollection((short)0);
        this.updateById(wmMaterialDB);

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 收藏
     *
     * @param id
     * @return
     */
    @Override
    public ResponseResult collect(Integer id) {
        //1. 判断数据是否空
        WmMaterial wmMaterial = this.getById(id);
        if(wmMaterial==null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        //2. 构建一个更新对象  update wm_material set is_collection=? where id=?
        WmMaterial wmMaterialDB = new WmMaterial();
        wmMaterialDB.setId(id);
        wmMaterialDB.setIsCollection((short)1);
        this.updateById(wmMaterialDB);

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
