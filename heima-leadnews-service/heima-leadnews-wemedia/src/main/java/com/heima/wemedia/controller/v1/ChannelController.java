package com.heima.wemedia.controller.v1;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmChannel;
import com.heima.wemedia.service.WmChannelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/channel")
public class ChannelController {

    @Autowired
    private WmChannelService channelService;

    /**
     * 查询频道列表
     * @return
     */
    @GetMapping("/channels")
    public ResponseResult<List<WmChannel>> list(){
        List<WmChannel> list = channelService.list(Wrappers.<WmChannel>lambdaQuery().eq(WmChannel::getStatus, 1));
        return ResponseResult.okResult(list);
    }
}
