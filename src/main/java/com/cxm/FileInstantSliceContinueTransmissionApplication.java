package com.cxm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.cxm.dao")
public class FileInstantSliceContinueTransmissionApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileInstantSliceContinueTransmissionApplication.class, args);
    }

}
