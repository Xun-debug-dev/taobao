package com.heima.api.feign;

import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.common.dtos.ResponseResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("leadnews-article")
public interface ApArticleFeignClient {

    /**
     * 保存三剑客
     * @return
     */
    @PostMapping("/api/v1/article/save")
    ResponseResult save(@RequestBody ArticleDto dto);
}
