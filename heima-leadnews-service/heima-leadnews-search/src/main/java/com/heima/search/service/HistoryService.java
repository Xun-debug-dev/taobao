package com.heima.search.service;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.search.dtos.HistorySearchDto;

public interface HistoryService {
    /**
     * 加载历史记录
     * @return
     */
    public ResponseResult load();
    /**
     * 删除历史记录
     * @param dto
     * @return
     */
    ResponseResult del(HistorySearchDto dto);
}
