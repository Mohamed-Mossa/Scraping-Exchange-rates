package com.example.demo.controller;

import com.example.demo.entity.ExchangeRate;
import com.example.demo.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService service;

    @GetMapping("/api/rates")
    public List<ExchangeRate> getRates() {
        return service.getAllRates();
    }

    @GetMapping("/api/rates/scrape")
    public List<ExchangeRate> scrapeRates() {
        return service.scrapeAndSaveRates();

    }
    @GetMapping("/api/rates/push")
    public String scrapeAndPushToOracle() {
        List<ExchangeRate> rates = service.scrapeAndSaveRates();
        service.pushValidRatesToOracle(rates);
        return "Scrape and insert to Oracle completed. Check logs for details.";
    }
}
