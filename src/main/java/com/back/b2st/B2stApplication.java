package com.back.b2st;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {
	io.awspring.cloud.autoconfigure.s3.S3AutoConfiguration.class,
	org.redisson.spring.starter.RedissonAutoConfigurationV4.class
})
public class B2stApplication {

	public static void main(String[] args) {
		SpringApplication.run(B2stApplication.class, args);
	}

}
