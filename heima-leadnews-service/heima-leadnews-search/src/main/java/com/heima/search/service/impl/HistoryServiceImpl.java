package com.heima.search.service.impl;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.search.dtos.HistorySearchDto;
import com.heima.search.pojos.ApUserSearch;
import com.heima.search.service.HistoryService;
import com.heima.utils.threadlocal.AppThreadLocalUtil;
import com.mongodb.client.result.DeleteResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HistoryServiceImpl implements HistoryService {
    @Autowired
    private MongoTemplate mongoTemplate;
    /**
     * 加载历史记录
     *
     * @return
     */
    @Override
    public ResponseResult load() {
        Integer id = AppThreadLocalUtil.getUser().getId();
        if(id==0){
            return ResponseResult.errorResult(AppHttpCodeEnum.NO_OPERATOR_AUTH);
        }
        Query query=Query.query(
                Criteria.where("userId").is(id)
        ).with(Sort.by(Sort.Order.desc("createdTime")));
        //每页显示5条
        query.limit(5);

        //查询mongo,根据用户id和时间倒序以及展示数量
        List<ApUserSearch> userSearchList = mongoTemplate.find(query, ApUserSearch.class);
        return ResponseResult.okResult(userSearchList);
    }

    /**
     * 删除历史记录
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult del(HistorySearchDto dto) {
        Integer id = AppThreadLocalUtil.getUser().getId();
        if(id==0){
            return ResponseResult.okResult(AppHttpCodeEnum.NO_OPERATOR_AUTH);
        }
        DeleteResult result = mongoTemplate.remove(
                Query.query(Criteria.where("id").is(dto.getId()).and("userId").is(id)),
                ApUserSearch.class
        );
        if (result.wasAcknowledged()) {
            return ResponseResult.okResult("删除成功");
        }

        return ResponseResult.errorResult(AppHttpCodeEnum.SERVER_ERROR,"删除历史记录失败");
    }
}
