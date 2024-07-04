package com.heima.audit.baidu;

import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.Setter;
import okhttp3.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Base64;


@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "baidu")
public class BaiduImageScan {

    private String apiKey;

    private String secretKey;

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient().newBuilder().build();


    /**
     * 获取文件base64编码
     * @return base64编码信息，不带文件头
     * @throws IOException IO异常
     */
    private String getFileContentAsBase64(byte[] bytes) throws IOException {
        return URLEncoder.encode(Base64.getEncoder().encodeToString(bytes),"utf-8") ;
    }


    /**
     * 审核图片
     */
    public  Integer imageScan(byte[] bytes){
        try {
            MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
            // image 可以通过 getFileContentAsBase64("C:\fakepath\3.png") 方法获取
            String base64 = getFileContentAsBase64(bytes);
            //System.out.println(base64);
            RequestBody body = RequestBody.create(mediaType, "image="+base64);
            Request request = new Request.Builder()
                    .url("https://aip.baidubce.com/rest/2.0/solution/v1/img_censor/v2/user_defined?access_token=" + getAccessToken())
                    .method("POST", body)
                    .addHeader("Content-Type", "application/x-www-form-urlencoded")
                    .addHeader("Accept", "application/json")
                    .build();
            Response response = HTTP_CLIENT.newCall(request).execute();
            String responseBody = response.body().string();
            System.out.println(responseBody);
            //响应结果里conclusionType的四种可能值：1.合规，2.不合规，3.疑似，4.审核失败
            return JSON.parseObject(responseBody).getInteger("conclusionType");
        } catch (IOException e) {
            e.printStackTrace();
            return 4;
        }
    }


    /**
     * 从用户的AK，SK生成鉴权签名（Access Token）
     *
     * @return 鉴权签名（Access Token）
     * @throws IOException IO异常
     */
    private  String getAccessToken() throws IOException{
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, "grant_type=client_credentials&client_id=" + apiKey
                + "&client_secret=" + secretKey);
        Request request = new Request.Builder()
                .url("https://aip.baidubce.com/oauth/2.0/token")
                .method("POST", body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();
        Response response = HTTP_CLIENT.newCall(request).execute();
        return JSON.parseObject(response.body().string()).getString("access_token");
    }
}