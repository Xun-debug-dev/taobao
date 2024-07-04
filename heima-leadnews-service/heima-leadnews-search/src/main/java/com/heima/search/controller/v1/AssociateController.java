package com.heima.search.controller.v1;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.search.dtos.UserSearchDto;
import com.heima.search.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/associate")
public class AssociateController {

    @Autowired
    private SearchService searchService;
    /**
     * 自动补全功能
     * @return
     */
    @PostMapping("/search")
    public ResponseResult load(@RequestBody UserSearchDto dto){
        return searchService.load(dto);
    }
}
