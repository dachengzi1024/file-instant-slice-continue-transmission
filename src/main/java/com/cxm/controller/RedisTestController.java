package com.cxm.controller;

import com.cxm.config.AjaxResult;
import com.cxm.utils.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @className: RedisTestController
 * @description:
 * @author: cxm
 * @date: 2024-11-26
 */
@RestController
@RequestMapping("redis")
public class RedisTestController {

    @Autowired
    private RedisCache redisCache;

    @PostMapping("save")
    public AjaxResult save(String key, String value){
        redisCache.setCacheObject(key, value);
        return AjaxResult.success();
    }

    @GetMapping("get")
    public AjaxResult get(String key){
        Object str = redisCache.getCacheObject(key);
        return AjaxResult.success(str);
    }

}
