package com.heima.article;

import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.article.mapper.ArticleContentMapper;
import com.heima.article.mapper.ArticleMapper;
import com.heima.file.service.FileStorageService;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.pojos.ApArticleContent;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
public class GenerHtmlTest {

    @Autowired
    ArticleMapper articleMapper;

    @Resource
    ArticleContentMapper articleContentMapper;

    @Autowired
    private Configuration configuration;

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * 测试生成静态化文件并上传到minio中
     */
    @Test
    public void generHtml() throws Exception {
        //1.根据文章id获取文章内容数据
        ApArticleContent apArticleContent = articleContentMapper.selectOne(Wrappers.<ApArticleContent>lambdaQuery().eq(ApArticleContent::getArticleId, 1302862387124125698L));
        if(apArticleContent==null){
            return;
        }
        //2.生成静态化文件
        Template template = configuration.getTemplate("article.ftl");

        Map dataModel=new HashMap();
        String content = apArticleContent.getContent();
        dataModel.put("content", JSONArray.parseArray(content));
        //声明字符串写入流对象，此时当前这个对象中是没有任何数据
        StringWriter out=new StringWriter();
        template.process(dataModel,out);//如果走完当前这行代码，此时写入流对象中就有了数据
        //3.上传到minio中
        InputStream inputStream=new ByteArrayInputStream(out.toString().getBytes());
        String path = fileStorageService.uploadHtmlFile("", apArticleContent.getArticleId() + ".html", inputStream);

        //4.修改ap_article表中static_url字段的值
        ApArticle entity=new ApArticle();
        entity.setId(apArticleContent.getArticleId());
        entity.setStaticUrl(path);
        articleMapper.updateById(entity);

        inputStream.close();
        out.close();
    }
}
