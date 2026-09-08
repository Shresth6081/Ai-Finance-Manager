package com.financemanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class FinanceManagerApplication {
	public static void main(String[] args) {
		SpringApplication.run(FinanceManagerApplication.class, args);
	}
}