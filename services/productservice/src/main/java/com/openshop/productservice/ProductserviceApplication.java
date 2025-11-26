package com.openshop.productservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@Slf4j
public class ProductserviceApplication {

	public static void main(String[] args) {
		log.info("Starting Product Service Application...");
		SpringApplication.run(ProductserviceApplication.class, args);
		log.info("Product Service Application started successfully");
	}
}
