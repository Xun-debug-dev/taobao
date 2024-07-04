package com.heima.search.service.impl;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.search.dtos.UserSearchDto;
import com.heima.search.service.SearchService;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private RestHighLevelClient client;

    /**
     * 基本搜索业务
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult search(UserSearchDto dto) {

        //1.判断参数是否为空
        if(StringUtils.isBlank(dto.getSearchWords())){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE);
        }

        SearchRequest searchRequest = new SearchRequest("app_info_article");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        //2.设置搜索条件 -- Bool组合搜索
        //条件1：根据关键词在title和content两个字段中搜索
        boolQueryBuilder.must(QueryBuilders.queryStringQuery(dto.getSearchWords()).field("title").field("content").defaultOperator(Operator.OR));

        //条件2：根据publishTime小于minBehotTime搜索（上拉翻页效果）
        boolQueryBuilder.filter(QueryBuilders.rangeQuery("publishTime").lt(dto.getMinBehotTime().getTime()));

        searchSourceBuilder.query(boolQueryBuilder);

        //3.设置搜索结果要求
        //要求1：根据publishTime倒排序
        searchSourceBuilder.sort("publishTime", SortOrder.DESC);

        //要求2：限制查询条数
        searchSourceBuilder.size(dto.getPageSize());

        //要求3：在title中进行高亮显示
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");
        highlightBuilder.preTags("<font style='color:red;font-size:inherit;'>");
        highlightBuilder.postTags("</font>");
        searchSourceBuilder.highlighter(highlightBuilder);
        searchRequest.source(searchSourceBuilder);


        List<Map<String, Object>> articleList = new ArrayList<>();
        try {
            //4.执行搜索请求得到响应实例
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

            //5.获取搜索命中的文档数组
            SearchHit[] searchHits = searchResponse.getHits().getHits();

            //6.遍历文档数组获取想要的文章数据
            if(searchHits!=null && searchHits.length>0){
                for (SearchHit hit : searchHits) {
                    Map<String, Object> articleMap = hit.getSourceAsMap();

                    Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                    if(highlightFields!=null && highlightFields.size()>0){
                        HighlightField highlightField = highlightFields.get("title");
                        Text[] fragments = highlightField.getFragments();
                        if(fragments!=null && fragments.length>0){
                            String hTitle = fragments[0].toString(); //带高亮标签的标题
                            articleMap.put("h_title", hTitle);
                        } else {
                            articleMap.put("h_title", articleMap.get("title"));
                        }
                    } else {
                        articleMap.put("h_title", articleMap.get("title"));
                    }

                    articleList.add(articleMap);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ResponseResult.okResult(articleList);
    }


    /**
     * 自动补全功能
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult load(UserSearchDto dto) {
       //TODO
        return null;
    }
}
