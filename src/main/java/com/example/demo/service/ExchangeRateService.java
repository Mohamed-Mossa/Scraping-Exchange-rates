package com.example.demo.service;

import com.example.demo.entity.ExchangeRate;
import com.example.demo.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private static final Logger logger = LoggerFactory.getLogger(ExchangeRateService.class);
    private final ExchangeRateRepository repository;
    private final JdbcTemplate jdbcTemplate;

    public List<ExchangeRate> scrapeAndSaveRates() {
        // ✅ Use the correct full path to your local ChromeDriver binary
        System.setProperty("webdriver.edge.driver", "C:\\driver\\msedgedriver.exe");

        EdgeOptions options = new EdgeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");

        WebDriver driver = new EdgeDriver(options);
        List<ExchangeRate> rates = new ArrayList<>();

        try {
            driver.get("https://www.nbe.com.eg/NBE/E/#/EN/ExchangeRatesAndCurrencyConverter");

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("table.currency-table")));

            String pageSource = driver.getPageSource();
            Document doc = Jsoup.parse(pageSource);

            for (Element row : doc.select("table.currency-table tbody tr")) {
                try {
                    Element currencyElement = row.selectFirst("td.currency-cell span.currency-name, td#flag span:last-child");
                    if (currencyElement == null) {
                        logger.warn("Skipping row - currency element not found: {}", row.html());
                        continue;
                    }

                    String currency = currencyElement.text().trim();
                    if (currency.isEmpty() || currency.equalsIgnoreCase("Currency")) {
                        continue;
                    }

                    var banknoteRows = row.select("td.banknote-cell table tr, td#Banknote table tr");
                    var transferRows = row.select("td.transfer-cell table tr, td#Transfer table tr");

                    if (banknoteRows.size() < 2 || transferRows.size() < 2) {
                        logger.warn("Skipping {} - insufficient data rows", currency);
                        continue;
                    }

                    String banknoteBuy = safeExtract(banknoteRows.get(0), 1);
                    String banknoteSell = safeExtract(banknoteRows.get(1), 1);
                    String transferBuy = safeExtract(transferRows.get(0), 1);
                    String transferSell = safeExtract(transferRows.get(1), 1);

                    if (banknoteBuy == null || banknoteSell == null ||
                            transferBuy == null || transferSell == null) {
                        logger.warn("Skipping {} - missing rate values", currency);
                        continue;
                    }

                    rates.add(ExchangeRate.builder()
                            .currency(currency)
                            .banknoteBuy(banknoteBuy)
                            .banknoteSell(banknoteSell)
                            .transferBuy(transferBuy)
                            .transferSell(transferSell)
                            .build());

                } catch (Exception e) {
                    logger.error("Error processing row: {}", e.getMessage());
                }
            }

            if (rates.isEmpty()) {
                logger.error("No rates extracted! Page structure may have changed");
            } else {
                repository.deleteAll();
                repository.saveAll(rates);
                logger.info("Successfully scraped {} currencies", rates.size());
            }

        } catch (Exception e) {
            logger.error("Scraping failed: {}", e.getMessage());
        } finally {
            driver.quit();
        }

        return rates;
    }

    public void pushValidRatesToOracle(List<ExchangeRate> rates) {
        String sql = "INSERT INTO GL_DAILY_RATES_INTERFACE (FROM_CURRENCY, TO_CURRENCY, FROM_CONVERSION_DATE, TO_CONVERSION_DATE, USER_CONVERSION_TYPE, CONVERSION_RATE, USER_ID, MODE_FLAG) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";



        LocalDate today = LocalDate.now();
        int insertedCount = 0;

        for (ExchangeRate rate : rates) {
            try {
                double transferBuy = Double.parseDouble(rate.getTransferBuy());
                double banknoteBuy = Double.parseDouble(rate.getBanknoteBuy());

                // Skip if both are zero
                if (transferBuy == 0 && banknoteBuy == 0) {
                    continue;
                }

                double conversionRate = (transferBuy != 0) ? transferBuy : banknoteBuy;

                // Print only the records that will be inserted
                System.out.printf("INSERTING: TO_CURRENCY=%s, CONVERSION_RATE=%.4f%n",
                        rate.getCurrency(), conversionRate);

                jdbcTemplate.update(sql,
                        rate.getCurrency(), // FROM_CURRENCY
                        "EGP",
                        java.sql.Date.valueOf(today),
                        java.sql.Date.valueOf(today),
                        "Corporate",
                        conversionRate,
                        1116,
                        "I"
                );

                insertedCount++;

            } catch (NumberFormatException e) {
                logger.warn("Skipping invalid numeric format for currency {}: {}", rate.getCurrency(), e.getMessage());
            } catch (Exception e) {
                logger.error("Failed to insert currency {}: {}", rate.getCurrency(), e.getMessage());
            }
        }

        logger.info("Finished inserting {} valid exchange rates.", insertedCount);
    }


    private String safeExtract(Element row, int cellIndex) {
        if (row == null) return null;
        List<Element> cells = row.select("td");
        return (cells.size() > cellIndex) ? cells.get(cellIndex).text().trim() : null;
    }

    public List<ExchangeRate> getAllRates() {
        return repository.findAll();
    }
}
