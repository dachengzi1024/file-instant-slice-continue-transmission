package com.cxm.controller;

import com.cxm.utils.MapUtils;
import com.cxm.utils.RedisCache;
import com.cxm.utils.StreamTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("upload")
public class FileUploadController {

    public static final String UPLOAD_PATH = "D:\\upload\\";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RedisCache redisCache;

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
            String key = "file_" + fileMd5;
            TimeUnit timeUnit = TimeUnit.DAYS;
            if (redisCache.getCacheObject(key) != null) {
                return "妙传成功";
            } else {
                redisCache.setCacheObject(key, "false");
                redisCache.setCacheObject("block_" + fileMd5, path);
                return "文件正在上传";
            }

        } catch (Exception e) {
            return "文件上传失败";
        }
    }

    private String getfileMd5(InputStream inputStream) throws NoSuchAlgorithmException, IOException, Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] hash = digest.digest(StreamTools.readInputStream(inputStream));
        return String.format("%032x", new BigInteger(1, hash));
    }

    /**
     * @param chunkSize   每个分片大小
     * @param chunkNumber 当前分片
     * @param md5         文件总MD5
     * @param file        当前分片文件数据
     * @param totalNumber 分片数
     * @return
     * @throws IOException
     */
    @PostMapping("/uploadBig")
    public ResponseEntity<Map<String, String>> UploadBig(@RequestParam Long chunkSize, @RequestParam Integer totalNumber, @RequestParam Long chunkNumber, @RequestParam String md5, @RequestParam MultipartFile file) throws IOException, Exception {
        //文件存放位置
        String dstFile = String.format("%s\\%s\\%s.%s", UPLOAD_PATH, md5, md5, StringUtils.getFilenameExtension(file.getOriginalFilename()));
        //上传分片信息存放位置
        String confFile = String.format("%s\\%s\\%s.conf", UPLOAD_PATH, md5, md5);

        File dir = new File(dstFile).getParentFile();
        if (!dir.exists()) {
            dir.mkdir();
            byte[] bytes = new byte[totalNumber];
            Files.write(Paths.get(confFile), bytes);
        }

        try (RandomAccessFile randomAccessFile = new RandomAccessFile(dstFile, "rw");
             RandomAccessFile randomAccessConfFile = new RandomAccessFile(confFile, "rw");
             InputStream inputStream = file.getInputStream()) {
            randomAccessFile.seek(chunkNumber*chunkSize);
            randomAccessFile.write(StreamTools.readInputStream(inputStream));
            randomAccessConfFile.seek(chunkNumber);
            randomAccessConfFile.write(1);
        }
        return ResponseEntity.ok(MapUtils.of("path", dstFile));
    }
    /**
     * 获取文件分片状态，检测文件MD5合法性
     * @param md5
     */
    @RequestMapping("/checkFile")
    public  ResponseEntity<Map<String, String>> checkFile(@RequestParam String md5) throws Exception {
        String uploadPath = String.format("%s\\%s\\%s.conf", UPLOAD_PATH, md5, md5);
        Path path = Paths.get(uploadPath);
        if (!Files.exists(path.getParent())) {
            return ResponseEntity.ok(MapUtils.of("msg", "文件未上传"));
        }
        StringBuilder stringBuilder = new StringBuilder();
        byte[] bytes = Files.readAllBytes(path);
        for (byte b : bytes) {
            stringBuilder.append(String.valueOf(b));
        }
        if (!stringBuilder.toString().contains("0")) {
            File file = new File(String.format("%s\\%s\\", UPLOAD_PATH, md5));
            File[] files = file.listFiles();
            String filePath = "";
            for (File f: files) {
                if (!f.getName().contains("conf")) {
                    filePath = f.getAbsolutePath();
                    try (InputStream inputStream = new FileInputStream(f)) {
                        String md5pwd = DigestUtils.md5DigestAsHex(inputStream);
                        if (!md5pwd.equalsIgnoreCase(md5))
                            return ResponseEntity.ok(MapUtils.of("msg", "文件上传失败"));
                    }
                }
            }
            return ResponseEntity.ok(MapUtils.of("path", filePath));
        } else {
            return  ResponseEntity.ok(MapUtils.of("chucks", stringBuilder.toString()));
        }
    }
}
