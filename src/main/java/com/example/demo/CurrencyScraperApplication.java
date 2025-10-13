package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class CurrencyScraperApplication extends SpringBootServletInitializer {
    public static void main(String[] args) {
        SpringApplication.run(CurrencyScraperApplication.class, args);
    }
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) { return application.sources(new Class[] { com.example.demo.CurrencyScraperApplication.class }); }
}
