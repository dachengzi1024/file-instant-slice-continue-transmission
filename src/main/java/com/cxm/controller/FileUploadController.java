package com.cxm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@RestController
public class FileUploadController {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @PostMapping("/upload")
    public String UploadFile(@RequestParam("file") MultipartFile file) {
        String result = "";
        try {
            Path path = Paths.get("uploads/" + file.getOriginalFilename());
            if (!Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            file.transferTo(path.toFile());
            String fileMd5 = getfileMd5(file.getInputStream());
        } catch (IOException | NoSuchAlgorithmException e) {
            return "文件上传失败";
        }
        return result;
    }

    private String getfileMd5(InputStream inputStream) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] hash = digest.digest(inputStream.readAllBytes());
        return String.format("%032x", new BigInteger(1, hash));
    }

}
