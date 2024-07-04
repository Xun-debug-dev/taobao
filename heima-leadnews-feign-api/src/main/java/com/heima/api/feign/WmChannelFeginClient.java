package com.heima.api.feign;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmChannel;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient("leadnews-wemedia")
public interface WmChannelFeginClient {
    /**
     * 查询频道列表
     * @return
     */
    @GetMapping("/api/v1/channel/channels")
    ResponseResult<List<WmChannel>>list();
}
