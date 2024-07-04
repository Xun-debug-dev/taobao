package com.heima.article;

import com.heima.file.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.Callable;

@SpringBootTest
public class MinioTest {
    @Autowired
    private FileStorageService fileStorageService;

    @Test
    public void uploadImage() throws Exception {
        /**
         * uploadImgFile(String prefix, String filename,InputStream inputStream)
         * 参数1标识前缀，置空处理
         * 参数2标识文件名称
         * 参数3表示文件流
         */
        String filename= UUID.randomUUID().toString().replace("-","")+".jpg";
        String path = fileStorageService.uploadImgFile("", filename, new FileInputStream("C:\\Users\\45502\\Desktop\\timg.jpg"));
        System.out.println(path);
    }

    @Test
    public void uploadHtml() throws Exception {
        String path = fileStorageService.uploadHtmlFile("", "abc.html", new FileInputStream("D:\\index.html"));
        System.out.println(path);
    }


    @Test
    void name() {
        LocalDateTime localDateTime = LocalDateTime.now().withMinute(0);



    }
}
