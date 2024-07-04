package com.heima.search.controller.v1;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.search.dtos.HistorySearchDto;
import com.heima.search.service.HistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/history")
public class HistoryController {

    @Autowired
    private HistoryService historyService;
    /**
     * 加载历史记录
     * @return
     */
    @PostMapping("/load")
    public ResponseResult load(){
        return historyService.load();
    }

    /**
     * 删除历史记录
     * @param dto
     * @return
     */
    @PostMapping("/del")
    public ResponseResult del(@RequestBody HistorySearchDto dto){
        return historyService.del(dto);
    }
}
