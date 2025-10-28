package com.example.demo.service;

import com.example.demo.entity.ExchangeRate;
import com.example.demo.repository.ExchangeRateRepository;
import jakarta.transaction.Transactional;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;



@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private static final Logger logger = LoggerFactory.getLogger(ExchangeRateService.class);
    private final ExchangeRateRepository repository;
    private final JdbcTemplate jdbcTemplate;

    @Qualifier("DB2JdbcTemplate")
    private final JdbcTemplate db2JdbcTemplate;



    private static final Map<String, String> currencyNameToCodeMap = Map.ofEntries(
            Map.entry("US DOLLAR", "USD"),
            Map.entry("EURO", "EUR"),
            Map.entry("POUND STERLING", "GBP"),
            Map.entry("CANADIAN DOLLAR", "CAD"),
            Map.entry("DANISH Krone", "DKK"),
            Map.entry("NORWEGIAN KRONE", "NOK"),
            Map.entry("Swedish Krona", "SEK"),
            Map.entry("SWISS FRANC", "CHF"),
            Map.entry("YENS(100)", "JPY"),
            Map.entry("AUSTRALIAN DOLLARS", "AUD"),
            Map.entry("Kuwait DINAR", "KWD"),
            Map.entry("SAUDI RIALS", "SAR"),
            Map.entry("U.A.E DIRHAM", "AED"),
            Map.entry("BAHRAIN DINAR", "BHD"),
            Map.entry("OMANI RIAL", "OMR"),
            Map.entry("QATAR RIAL", "QAR"),
            Map.entry("JORDAN DINAR", "JOD"),
            Map.entry("Egyptian Pound", "EGP")
    );
    private static final Map<String, String> currencyNameToCodeMapAs400 = Map.ofEntries(
            Map.entry("US DOLLAR", "US"),
            Map.entry("EURO", "EU"),
            Map.entry("POUND STERLING", "SL"), // British Pound
            Map.entry("CANADIAN DOLLAR", "CD"),
            Map.entry("DANISH Krone", "DK"),
            Map.entry("NORWEGIAN KRONE", "NK"),
            Map.entry("Swedish Krona", "SK"),
            Map.entry("SWISS FRANC", "CH"),
            Map.entry("YENS(100)", "JY"),
            Map.entry("AUSTRALIAN DOLLARS", "AD"),
            Map.entry("Kuwait DINAR", "KD"),
            Map.entry("SAUDI RIALS", "SR"),
            Map.entry("U.A.E DIRHAM", "AE"),
            Map.entry("BAHRAIN DINAR", "BD"),
            Map.entry("OMANI RIAL", "OR"),
            Map.entry("QATAR RIAL", "QR"),
            Map.entry("JORDAN DINAR", "JD"),
            Map.entry("Egyptian Pound", "LE")
    );


    public boolean isOracleDataAlreadyPushedToday(LocalDate today) {
        LocalDate targetDate = today.plusDays(1);
        String sql = "SELECT COUNT(*) FROM apps.GL_DAILY_RATES_INTERFACE WHERE FROM_CONVERSION_DATE = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, java.sql.Date.valueOf(targetDate));
        return count != null && count > 0;
    }

    public boolean isOracleAlreadyPushedToday(LocalDate today) {
        LocalDate targetDate = today.plusDays(1);
        String sql = "SELECT COUNT(*) FROM apps.GL_DAILY_RATES WHERE CONVERSION_DATE = ? AND TO_CURRENCY ='EGP'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, java.sql.Date.valueOf(targetDate));
        return count != null && count > 0;
    }

    // ✅ NEW: Check if Oracle already has data for a specific date
    public boolean isOracleAlreadyPushedForDate(LocalDate targetDate) {
        String sql = "SELECT COUNT(*) FROM apps.GL_DAILY_RATES WHERE CONVERSION_DATE = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, java.sql.Date.valueOf(targetDate));
        return count != null && count > 0;
    }

    public boolean isAs400DataAlreadyPushedToday(LocalDate today) {
        LocalDate targetDate = today.plusDays(1);

        int year = targetDate.getYear() ;
        int month = targetDate.getMonthValue();
        int day = targetDate.getDayOfMonth();

        String sql = "SELECT COUNT(*) FROM ACCOUNT.EXRATE WHERE EXYY = ? AND EXMM = ? AND EXDD = ?";

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            connection = Utils.getAS400Connection();
            if (connection == null) {
                logger.error("AS400 connection is null. Cannot check existing data.");
                return false;
            }

            ps = connection.prepareStatement(sql);
            ps.setInt(1, year);
            ps.setInt(2, month);
            ps.setInt(3, day);

            rs = ps.executeQuery();

            if (rs.next()) {
                int count = rs.getInt(1);
                return count > 0;
            }

        } catch (SQLException e) {
            logger.error("Error checking AS400 data: {}", e.getMessage(), e);
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                Utils.closeAS400Connection(connection);
            } catch (SQLException e) {
                logger.warn("Error closing AS400 resources: {}", e.getMessage());
            }
        }

        return false;
    }
    // ✅ NEW: Check if AS400 already has data for a specific date
    public boolean isAs400AlreadyPushedForDate(LocalDate targetDate) {
        int year = targetDate.getYear() ;
        int month = targetDate.getMonthValue();
        int day = targetDate.getDayOfMonth();

        String sql = "SELECT COUNT(*) FROM ACCOUNT.EXRATE WHERE EXYY = ? AND EXMM = ? AND EXDD = ?";

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            connection = Utils.getAS400Connection();
            if (connection == null) {
                logger.error("AS400 connection is null. Cannot check existing data.");
                return false;
            }

            ps = connection.prepareStatement(sql);
            ps.setInt(1, year);
            ps.setInt(2, month);
            ps.setInt(3, day);

            rs = ps.executeQuery();

            if (rs.next()) {
                int count = rs.getInt(1);
                return count > 0;
            }

        } catch (SQLException e) {
            logger.error("Error checking AS400 data for date {}: {}", targetDate, e.getMessage(), e);
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                Utils.closeAS400Connection(connection);
            } catch (SQLException e) {
                logger.warn("Error closing AS400 resources: {}", e.getMessage());
            }
        }

        return false;
    }

    @Scheduled(cron = "0 */10 16-21 * * *", zone = "Africa/Cairo")
    public void scheduledScrapeIfNotExists() {
        LocalDate today = LocalDate.now();

        // Check if any rate was scraped today
        boolean hasTodayData = repository.existsByScrapedAtBetween(
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );

        if (hasTodayData) {
            logger.info("Exchange rates already scraped today. No action taken.");
            if (!(isOracleDataAlreadyPushedToday(today)||isOracleAlreadyPushedToday(today))) {
                logger.info("Data not pushed to Oracle yet. Fetching from DB and pushing now...");
                List<ExchangeRate> rates = repository.findByScrapedAtBetween(
                        today.atStartOfDay(),
                        today.plusDays(1).atStartOfDay()
                );
                int r = pushValidRatesToOracle(rates, today);
                if(r > 0){
                    sendCompletionEmail("Oracle",rates);
                }
            } else {
                logger.info("Data already pushed to Oracle. No action needed.");
            }
            if (!isAs400DataAlreadyPushedToday(today)) {
                logger.info("Data not pushed to AS400 yet. Fetching from DB and pushing now...");
                List<ExchangeRate> rates = repository.findByScrapedAtBetween(
                        today.atStartOfDay(),
                        today.plusDays(1).atStartOfDay()
                );
                int s = pushValidRatesToAs400(rates, today);
                if(s > 0){
                    sendCompletionEmail("AS400",rates);
                }
            } else {
                logger.info("Data already pushed to AS400. No action needed.");
            }

        }

        else {

            logger.info("No rates found for today. Starting scheduled scrape at {}", LocalDateTime.now());
            scrapeAndSaveRates();
        }
    }

    @Scheduled(cron = "0 40 16 * * *", zone = "Africa/Cairo")
    public void scheduledoracleTestrates(){
        LocalDate today = LocalDate.now();
        if (!isOracleAlreadyPushedToday(today)) {
            logger.info("Data not pushed to Oracle yet. Fetching from DB and pushing now...");

            sendFailedEmail("Oracle");
        } else {
            logger.info("Data already pushed to Oracle. No action needed.");
        }
    }
    // ✅ NEW: Push back days for Oracle (auto-fill missing dates)
    @Transactional
    public Map<String, Object> pushBackDaysToOracle(int daysBack) {
        LocalDate today = LocalDate.now();
        int successCount = 0;
        int skippedCount = 0;
        List<String> processedDates = new ArrayList<>();
        List<String> skippedDates = new ArrayList<>();

        logger.info("Starting Oracle back days push for {} days", daysBack);

        for (int i = 0; i <= daysBack; i++) {
            LocalDate processDate = today.minusDays(i);
            LocalDate targetDate = processDate.plusDays(1);

            // Check if already exists in Oracle
            if (isOracleAlreadyPushedForDate(targetDate)) {
                logger.info("Oracle already has data for target date: {}, skipping", targetDate);
                skippedCount++;
                skippedDates.add(targetDate.toString());
                continue;
            }

            // Get rates from our database
            List<ExchangeRate> rates = repository.findByScrapedAtBetween(
                    processDate.atStartOfDay(),
                    processDate.plusDays(1).atStartOfDay()
            );

            if (rates.isEmpty()) {
                logger.warn("No scraped data found for date: {}, skipping", processDate);
                skippedCount++;
                skippedDates.add(processDate.toString() + " (no data)");
                continue;
            }

            // Push to Oracle
            int inserted = pushValidRatesToOracle(rates, processDate);
            if (inserted > 0) {
                successCount++;
                processedDates.add(targetDate.toString() + " (" + inserted + " records)");
                logger.info("Successfully pushed {} rates to Oracle for target date: {}", inserted, targetDate);
            }
        }

        Map<String, Object> result = Map.of(
                "totalDaysProcessed", daysBack + 1,
                "successfulPushes", successCount,
                "skippedDates", skippedCount,
                "processedDates", processedDates,
                "skippedDatesList", skippedDates
        );

        logger.info("Oracle back days push completed: {}", result);
        return result;
    }

    // ✅ NEW: Push back days for AS400 (auto-fill missing dates)
    @Transactional
    public Map<String, Object> pushBackDaysToAs400(int daysBack) {
        LocalDate today = LocalDate.now();
        int successCount = 0;
        int skippedCount = 0;
        List<String> processedDates = new ArrayList<>();
        List<String> skippedDates = new ArrayList<>();

        logger.info("Starting AS400 back days push for {} days", daysBack);

        for (int i = 0; i <= daysBack; i++) {
            LocalDate processDate = today.minusDays(i);
            LocalDate targetDate = processDate.plusDays(1);

            // Check if already exists in AS400
            if (isAs400AlreadyPushedForDate(targetDate)) {
                logger.info("AS400 already has data for target date: {}, skipping", targetDate);
                skippedCount++;
                skippedDates.add(targetDate.toString());
                continue;
            }

            // Get rates from our database
            List<ExchangeRate> rates = repository.findByScrapedAtBetween(
                    processDate.atStartOfDay(),
                    processDate.plusDays(1).atStartOfDay()
            );

            if (rates.isEmpty()) {
                logger.warn("No scraped data found for date: {}, skipping", processDate);
                skippedCount++;
                skippedDates.add(processDate.toString() + " (no data)");
                continue;
            }

            // Push to AS400
            int inserted = pushValidRatesToAs400(rates, processDate);
            if (inserted > 0) {
                successCount++;
                processedDates.add(targetDate.toString() + " (" + inserted + " records)");
                logger.info("Successfully pushed {} rates to AS400 for target date: {}", inserted, targetDate);
            }
        }

        Map<String, Object> result = Map.of(
                "totalDaysProcessed", daysBack + 1,
                "successfulPushes", successCount,
                "skippedDates", skippedCount,
                "processedDates", processedDates,
                "skippedDatesList", skippedDates
        );

        logger.info("AS400 back days push completed: {}", result);
        return result;
    }

    // ✅ NEW: Push back days to BOTH systems
    @Transactional
    public Map<String, Object> pushBackDaysToBothSystems(int daysBack) {
        logger.info("Starting back days push to BOTH systems for {} days", daysBack);

        Map<String, Object> oracleResult = pushBackDaysToOracle(daysBack);
        Map<String, Object> as400Result = pushBackDaysToAs400(daysBack);

        return Map.of(
                "oracle", oracleResult,
                "as400", as400Result,
                "totalDaysProcessed", daysBack + 1
        );
    }




    public List<ExchangeRate> scrapeAndSaveRates() {
        // ✅ Use the correct full path to your local ChromeDriver binary
        System.setProperty("webdriver.edge.driver", "C:\\edgedriver\\msedgedriver.exe");//C:\edgedriver\msedgedriver.exe ...driver/msedgedriver.exe

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
            Element firstTable = doc.select("table.currency-table").first();

            for (Element row : firstTable.select("tbody tr")) {
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
                    LocalDateTime scrapedAt = LocalDateTime.now();
                    if (currency.equalsIgnoreCase("YENS(100)")) {
                        try {
                            if (banknoteBuy != null) {
                                double val = Double.parseDouble(banknoteBuy) / 100.0;
                                banknoteBuy = String.format("%.6f", val);
                            }
                            if (banknoteSell != null) {
                                double val = Double.parseDouble(banknoteSell) / 100.0;
                                banknoteSell = String.format("%.6f", val);
                            }
                            if (transferBuy != null) {
                                double val = Double.parseDouble(transferBuy) / 100.0;
                                transferBuy = String.format("%.6f", val);
                            }
                            if (transferSell != null) {
                                double val = Double.parseDouble(transferSell) / 100.0;
                                transferSell = String.format("%.6f", val);
                            }
                        } catch (NumberFormatException e) {
                            logger.warn("Invalid numeric format for YENS(100): {}", e.getMessage());
                        }
                    }
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
                            .scrapedAt(scrapedAt)
                            .build());

                } catch (Exception e) {
                    logger.error("Error processing row: {}", e.getMessage());
                }
            }

            if (rates.isEmpty()) {
                logger.error("No rates extracted! Page structure may have changed");
            } else {
//                repository.deleteAll();
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

    public int pushValidRatesToOracle(List<ExchangeRate> rates, LocalDate today) {
        String sql = "INSERT INTO apps.GL_DAILY_RATES_INTERFACE (FROM_CURRENCY, TO_CURRENCY, FROM_CONVERSION_DATE, TO_CONVERSION_DATE, USER_CONVERSION_TYPE, CONVERSION_RATE, USER_ID, MODE_FLAG) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";


        LocalDate tommorrow = today.plusDays(1);
        int insertedCount = 0;

        for (ExchangeRate rate : rates) {
            if(currencyNameToCodeMap.get(rate.getCurrency()) == null){
                continue;
            }
            try {
                double transferBuy = Double.parseDouble(rate.getTransferSell());// sell or buy
                double banknoteBuy = Double.parseDouble(rate.getBanknoteSell());

                // Skip if both are zero
                if (transferBuy == 0 && banknoteBuy == 0) {
                    continue;
                }

                double conversionRate = (transferBuy != 0) ? transferBuy : banknoteBuy;

                // Print only the records that will be inserted
                System.out.printf("INSERTING: TO_CURRENCY=%s, CONVERSION_RATE=%.4f%n",
                        rate.getCurrency(), conversionRate);

                jdbcTemplate.update(sql,
                        currencyNameToCodeMap.getOrDefault(rate.getCurrency(),null), // FROM_CURRENCY
                        "EGP",
                        java.sql.Date.valueOf(tommorrow),
                        java.sql.Date.valueOf(tommorrow),
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

        return insertedCount;
    }

    public int pushValidRatesToAs400(List<ExchangeRate> rates, LocalDate today) {
        String sql = "INSERT INTO ACCOUNT.EXRATE (EXYY, EXMM, EXDD, EXCUR, EXAMT) VALUES (?, ?, ?, ?, ?)";

        LocalDate tommorrow = today.plusDays(1);
        int insertedCount = 0;

        Connection connection = null;
        try {
            connection = Utils.getAS400Connection();
            if (connection == null) {
                logger.error("AS400 connection is null. Aborting push.");
                return insertedCount;
            }

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                for (ExchangeRate rate : rates) {
                    try {
                        double transferBuy = Double.parseDouble(rate.getTransferSell());
                        double banknoteBuy = Double.parseDouble(rate.getBanknoteSell());

                        if (transferBuy == 0 && banknoteBuy == 0) {
                            continue;
                        }

                        double conversionRate = (transferBuy != 0) ? transferBuy : banknoteBuy;

                        int year = tommorrow.getYear()  ;
                        int month = tommorrow.getMonthValue();
                        int day = tommorrow.getDayOfMonth();
                        String currencyCode = currencyNameToCodeMapAs400.getOrDefault(rate.getCurrency(), null);

                        // Debug logs
                        System.out.printf("AS400 INSERT: EXCUR=%s, EXAMT=%.4f, Date=%02d-%02d-%02d%n",
                                currencyCode, conversionRate, day, month, year);

                        ps.setInt(1, year);
                        ps.setInt(2, month);
                        ps.setInt(3, day);
                        ps.setString(4, currencyCode);
                        ps.setDouble(5, conversionRate);

                        ps.executeUpdate();
                        insertedCount++;

                    } catch (NumberFormatException e) {
                        logger.warn("Skipping invalid number for currency {}: {}", rate.getCurrency(), e.getMessage());
                    } catch (SQLException e) {
                        logger.error("Failed to insert currency {} into AS400: {}", rate.getCurrency(), e.getMessage());
                    }
                }

            }

        } catch (Exception e) {
            logger.error("Error connecting to AS400: {}", e.getMessage(), e);
        } finally {
            Utils.closeAS400Connection(connection);
        }

        logger.info("Finished inserting {} valid exchange rates into AS400.", insertedCount);
        return insertedCount;
    }

    private String safeExtract(Element row, int cellIndex) {
        if (row == null) return null;
        List<Element> cells = row.select("td");
        return (cells.size() > cellIndex) ? cells.get(cellIndex).text().trim() : null;
    }

    public List<ExchangeRate> getAllRates() {
        return repository.findAll();
    }

    // if you want only by date
    public List<ExchangeRate> getAllRatesByDate(LocalDate today) {
        return  repository.findByScrapedAtBetween(
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );
    }

    private void sendCompletionEmail(String systemName, List<ExchangeRate> rates) {
        List<Map<String, Object>> recipients = List.of(
                Map.of("EMAIL_ADDRESS", "mmousa@ezzsteel.com.eg"),


        );


        String subject = "Exchange Rate Push to " + systemName + " Completed";
        StringBuilder messageBuilder = new StringBuilder();

        messageBuilder.append("<html><body>");
        messageBuilder.append("<p>Exchange rate push to ").append(systemName).append(" completed.<br/>")
                .append("Time: ").append(LocalDateTime.now()).append("</p>");

        // Start table with border and some styling
        messageBuilder.append("<table border='1' cellpadding='5' cellspacing='0' style='border-collapse: collapse;'>");
        messageBuilder.append("<thead>");
        messageBuilder.append("<tr style='background-color:#f2f2f2;'>")
                .append("<th>Currency</th>")
                .append("<th>Transfer Sell</th>")
                .append("</tr>");
        messageBuilder.append("</thead>");
        messageBuilder.append("<tbody>");

        for (ExchangeRate rate : rates) {
            double transferSell = Double.parseDouble(rate.getTransferSell());
            double banknoteSell = Double.parseDouble(rate.getBanknoteSell());

            // Skip if both are zero
            if (transferSell == 0 && banknoteSell == 0) {
                continue;
            }

            double conversionRate = (transferSell != 0) ? transferSell : banknoteSell;

            messageBuilder.append("<tr>")
                    .append("<td>").append(rate.getCurrency()).append("</td>")
                    .append("<td>").append(conversionRate).append("</td>")
                    .append("</tr>");
        }

        messageBuilder.append("</tbody>");
        messageBuilder.append("</table>");
        messageBuilder.append("</body></html>");

        String message = messageBuilder.toString();

        String cc = String.join(",",
                "mmousa@ezzsteel.com.eg"



        );
        String attachmentId = "";

        String result = Utils.sendEmail(recipients, subject, message, cc, attachmentId);
        logger.info("Email sent result: {}", result);
    }
    private void sendFailedEmail(String systemName) {
        List<Map<String, Object>> recipients = List.of(
                Map.of("EMAIL_ADDRESS", "mmousa@ezzsteel.com.eg")

        );


        String subject = "Exchange Rate Not Pushed to " + systemName + " Please Contact IT";
        StringBuilder messageBuilder = new StringBuilder();

        messageBuilder.append("<html><body>");
        messageBuilder.append("<p style='color:red; font-weight:bold;'>Exchange rate Not Pushed to ").append(systemName).append(" Please Contact IT.<br/>");


        messageBuilder .append("</p>");

        messageBuilder.append("</tbody>");

        messageBuilder.append("</body></html>");

        String message = messageBuilder.toString();

        String cc = String.join(",",
                "mmousa@ezzsteel.com.eg"


        );
        String attachmentId = "";

        String result = Utils.sendEmail(recipients, subject, message, cc, attachmentId);
        logger.info("Email sent result: {}", result);
    }


    public void sendTestEmail() {
        sendFailedEmail("Test-System");
    }
}