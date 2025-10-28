package com.example.demo.controller;

import com.example.demo.entity.ExchangeRate;
import com.example.demo.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rates")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService service;

    /**
     * Get all exchange rates from database
     */
    @GetMapping
    public ResponseEntity<List<ExchangeRate>> getRates() {
        return ResponseEntity.ok(service.getAllRates());
    }

    /**
     * Trigger manual scraping of exchange rates from NBE website
     */
    @GetMapping("/scrape")
    public ResponseEntity<Map<String, Object>> scrapeRates() {
        List<ExchangeRate> rates = service.scrapeAndSaveRates();
        return ResponseEntity.ok(Map.of(
                "message", "Scraping completed",
                "recordsScraped", rates.size(),
                "rates", rates
        ));
    }

    /**
     * Push exchange rates to Oracle for a specific date
     * @param days Number of days back from today (0 = today, 1 = yesterday, etc.)
     */
    @GetMapping("/push/oracle")
    public ResponseEntity<Map<String, Object>> pushToOracle(
            @RequestParam(defaultValue = "0") int days) {
        LocalDate targetDate = LocalDate.now().minusDays(days);
        List<ExchangeRate> rates = service.getAllRatesByDate(targetDate);

        if (rates.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "message", "No data found for date: " + targetDate,
                    "recordsPushed", 0
            ));
        }

        int pushed = service.pushValidRatesToOracle(rates, targetDate);
        return ResponseEntity.ok(Map.of(
                "message", "Oracle push completed",
                "targetDate", targetDate.plusDays(1),
                "recordsPushed", pushed
        ));
    }

    /**
     * Push exchange rates to AS400 for a specific date
     * @param days Number of days back from today (0 = today, 1 = yesterday, etc.)
     */
    @GetMapping("/push/as400")
    public ResponseEntity<Map<String, Object>> pushToAs400(
            @RequestParam(defaultValue = "0") int days) {
        LocalDate targetDate = LocalDate.now().minusDays(days);
        List<ExchangeRate> rates = service.getAllRatesByDate(targetDate);

        if (rates.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "message", "No data found for date: " + targetDate,
                    "recordsPushed", 0
            ));
        }

        int pushed = service.pushValidRatesToAs400(rates, targetDate);
        return ResponseEntity.ok(Map.of(
                "message", "AS400 push completed",
                "targetDate", targetDate.plusDays(1),
                "recordsPushed", pushed
        ));
    }

    /**
     * ✅ NEW: Push back days to Oracle (auto-fill missing dates)
     * Example: /api/rates/push/oracle/backfill?daysBack=7
     * This will process last 7 days and skip dates that already exist
     *
     * @param daysBack Number of days to go back (e.g., 7 = last week, 30 = last month)
     */
    @GetMapping("/push/oracle/backfill")
    public ResponseEntity<Map<String, Object>> pushBackDaysToOracle(
            @RequestParam(defaultValue = "7") int daysBack) {

        if (daysBack < 0 || daysBack > 365) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "daysBack must be between 0 and 365"
            ));
        }

        Map<String, Object> result = service.pushBackDaysToOracle(daysBack);
        return ResponseEntity.ok(result);
    }

    /**
     * ✅ NEW: Push back days to AS400 (auto-fill missing dates)
     * Example: /api/rates/push/as400/backfill?daysBack=7
     * This will process last 7 days and skip dates that already exist
     *
     * @param daysBack Number of days to go back (e.g., 7 = last week, 30 = last month)
     */
    @GetMapping("/push/as400/backfill")
    public ResponseEntity<Map<String, Object>> pushBackDaysToAs400(
            @RequestParam(defaultValue = "7") int daysBack) {

        if (daysBack < 0 || daysBack > 365) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "daysBack must be between 0 and 365"
            ));
        }

        Map<String, Object> result = service.pushBackDaysToAs400(daysBack);
        return ResponseEntity.ok(result);
    }

    /**
     * ✅ NEW: Push back days to BOTH Oracle and AS400
     * Example: /api/rates/push/both/backfill?daysBack=7
     * This will process last 7 days for both systems and skip dates that already exist
     *
     * @param daysBack Number of days to go back (e.g., 7 = last week, 30 = last month)
     */
    @GetMapping("/push/both/backfill")
    public ResponseEntity<Map<String, Object>> pushBackDaysToBoth(
            @RequestParam(defaultValue = "7") int daysBack) {

        if (daysBack < 0 || daysBack > 365) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "daysBack must be between 0 and 365"
            ));
        }

        Map<String, Object> result = service.pushBackDaysToBothSystems(daysBack);
        return ResponseEntity.ok(result);
    }

    /**
     * Check if Oracle has data for today's push
     */
    @GetMapping("/status/oracle")
    public ResponseEntity<Map<String, Object>> checkOracleStatus() {
        LocalDate today = LocalDate.now();
        boolean hasData = service.isOracleAlreadyPushedToday(today);

        return ResponseEntity.ok(Map.of(
                "system", "Oracle",
                "date", today,
                "targetDate", today.plusDays(1),
                "hasData", hasData,
                "status", hasData ? "Data already pushed" : "No data pushed yet"
        ));
    }

    /**
     * Check if AS400 has data for today's push
     */
    @GetMapping("/status/as400")
    public ResponseEntity<Map<String, Object>> checkAs400Status() {
        LocalDate today = LocalDate.now();
        boolean hasData = service.isAs400DataAlreadyPushedToday(today);

        return ResponseEntity.ok(Map.of(
                "system", "AS400",
                "date", today,
                "targetDate", today.plusDays(1),
                "hasData", hasData,
                "status", hasData ? "Data already pushed" : "No data pushed yet"
        ));
    }

    /**
     * Get exchange rates for a specific date
     */
    @GetMapping("/by-date")
    public ResponseEntity<Map<String, Object>> getRatesByDate(
            @RequestParam(defaultValue = "0") int daysBack) {
        LocalDate targetDate = LocalDate.now().minusDays(daysBack);
        List<ExchangeRate> rates = service.getAllRatesByDate(targetDate);

        return ResponseEntity.ok(Map.of(
                "date", targetDate,
                "recordCount", rates.size(),
                "rates", rates
        ));
    }

    /**
     * Send test email
     */
    @GetMapping("/test-email")
    public ResponseEntity<String> sendTestEmail() {
        service.sendTestEmail();
        return ResponseEntity.ok("Test email sent. Check logs for confirmation.");
    }
}