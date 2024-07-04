package com.heima.wemedia;

import com.heima.audit.tess4j.Tess4jClient;
import com.heima.file.service.FileStorageService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

@SpringBootTest
@RunWith(SpringRunner.class)
public class OCRTest {

    @Autowired
    Tess4jClient tess4jClient;

    @Autowired
    FileStorageService fileStorageService;


    /**
     * 测试OCR图片识别
     */
    @Test
    public void testOCR() throws Exception {

        byte[] bytes = fileStorageService.downLoadFile("http://192.168.200.128:9000/leadnews/2023/09/27/a02da3ad2f064c00b6646aea5a6572d1.png");

        //图片识别文字审核---begin-----

        //从byte[]转换为butteredImage
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        BufferedImage imageFile = ImageIO.read(in);
        //识别图片的文字
        String result = tess4jClient.doOCR(imageFile);
        System.out.println("图片文字识别结果："+result);
    }
}
