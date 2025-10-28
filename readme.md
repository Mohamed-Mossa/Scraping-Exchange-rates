# 💱 Enterprise Currency Exchange Rate Integration System

A production-grade Spring Boot application that automatically scrapes real-time currency exchange rates from the National Bank of Egypt (NBE), stores them in a database, and seamlessly integrates with enterprise systems (Oracle E-Business Suite and IBM AS400) with automated scheduling, backfill capabilities, and email notifications.

## 🌟 Key Features

### Core Functionality
- **Automated Web Scraping**: Selenium-based scraping with intelligent retry logic
- **Scheduled Execution**: Runs every 10 minutes (4-9 PM Cairo time) with duplicate prevention
- **Dual Database Integration**: Pushes rates to both Oracle GL and IBM AS400
- **Smart Backfill**: Automatically fills missing dates (up to 365 days back)
- **Email Notifications**: Sends detailed reports when rates are successfully pushed
- **Status Monitoring**: Check if data already exists before pushing

### Production-Ready Features
- **Duplicate Prevention**: Checks if data already exists before inserting
- **Data Validation**: Filters invalid rates and handles edge cases (e.g., YENS/100 division)
- **Currency Mapping**: Converts full currency names to standard codes (ISO for Oracle, custom for AS400)
- **Error Handling**: Comprehensive logging with detailed error messages
- **Transaction Safety**: Uses @Transactional for data integrity

## 🛠️ Tech Stack

- **Java 17** - Latest LTS version
- **Spring Boot 3.2.1** - Modern Spring framework
- **Spring Data JPA** - Database abstraction
- **Spring Scheduling** - Cron-based task scheduling
- **Selenium WebDriver** (Edge) - Browser automation
- **Jsoup** - HTML parsing
- **Oracle Database** - Enterprise ERP integration (GL_DAILY_RATES_INTERFACE)
- **IBM AS400** - Legacy system integration (ACCOUNT.EXRATE)
- **H2/SQL Server** - Application database
- **Lombok** - Reduce boilerplate code
- **Maven** - Dependency management

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.6+
- Microsoft Edge browser
- Oracle Database (for production)
- IBM AS400 connection (for production)

## 🚀 Getting Started

### 1. Clone and Build

```bash
git clone https://github.com/yourusername/enterprise-exchange-rates.git
cd enterprise-exchange-rates
mvn clean install
```

### 2. Configure Database Connections

Edit `application.properties`:

```properties
# Oracle Configuration
spring.datasource.url=jdbc:oracle:thin:@//host:port/service
spring.datasource.username=your_username
spring.datasource.password=your_password

# AS400 Configuration (in Utils.java)
# Configure connection details for AS400
```

### 3. Run the Application

```bash
mvn spring-boot:run
```

The application will:
- Start on `http://localhost:8080`
- Automatically scrape rates every 10 minutes (4-9 PM Cairo time)
- Push to Oracle and AS400 if not already done today

## 📡 API Endpoints

### Get All Rates
```http
GET /api/rates
```
Returns all exchange rates from the database.

### Get Rates by Date
```http
GET /api/rates/by-date?daysBack=0
```
- `daysBack=0` → Today's rates
- `daysBack=1` → Yesterday's rates

**Response:**
```json
{
  "date": "2025-10-28",
  "recordCount": 18,
  "rates": [
    {
      "id": 1,
      "currency": "US DOLLAR",
      "banknoteBuy": "49.50",
      "banknoteSell": "49.60",
      "transferBuy": "49.45",
      "transferSell": "49.55",
      "scrapedAt": "2025-10-28T16:00:00"
    }
  ]
}
```

### Manual Scrape
```http
GET /api/rates/scrape
```
Manually trigger scraping (useful for testing).

### Push to Oracle
```http
GET /api/rates/push/oracle?days=0
```
Push specific day's rates to Oracle.

### Push to AS400
```http
GET /api/rates/push/as400?days=0
```
Push specific day's rates to AS400.

### Backfill Missing Dates (Oracle)
```http
GET /api/rates/push/oracle/backfill?daysBack=7
```
**Smart backfill**: Processes last 7 days, automatically skips dates that already exist.

**Response:**
```json
{
  "totalDaysProcessed": 8,
  "successfulPushes": 5,
  "skippedDates": 3,
  "processedDates": [
    "2025-10-29 (18 records)",
    "2025-10-28 (18 records)"
  ],
  "skippedDatesList": [
    "2025-10-27",
    "2025-10-26 (no data)"
  ]
}
```

### Backfill Missing Dates (AS400)
```http
GET /api/rates/push/as400/backfill?daysBack=7
```

### Backfill Both Systems
```http
GET /api/rates/push/both/backfill?daysBack=7
```
Backfills missing dates for **both Oracle and AS400** in one call.

### Check Status
```http
GET /api/rates/status/oracle
GET /api/rates/status/as400
```

**Response:**
```json
{
  "system": "Oracle",
  "date": "2025-10-28",
  "targetDate": "2025-10-29",
  "hasData": true,
  "status": "Data already pushed"
}
```

### Test Email
```http
GET /api/rates/test-email
```
Send a test email notification.

## 🏗️ System Architecture

```
┌─────────────────────────────────────────────────────────┐
│           Spring Boot Application                       │
│  ┌─────────────────────────────────────────────────┐   │
│  │  @Scheduled Job (Every 10 min, 4-9 PM Cairo)    │   │
│  └───────────────────┬─────────────────────────────┘   │
│                      │                                   │
│  ┌───────────────────▼─────────────────────────────┐   │
│  │  ExchangeRateController (REST API)              │   │
│  └───────────────────┬─────────────────────────────┘   │
│                      │                                   │
│  ┌───────────────────▼─────────────────────────────┐   │
│  │  ExchangeRateService                            │   │
│  │  • Scraping Logic (Selenium + Jsoup)            │   │
│  │  • Duplicate Check                              │   │
│  │  • Currency Mapping                             │   │
│  │  • Email Notifications                          │   │
│  └───────┬────────────────────┬───────────┬────────┘   │
│          │                    │           │             │
│  ┌───────▼────────┐  ┌────────▼───────┐ ┌▼──────────┐ │
│  │ JPA Repository │  │ Oracle JDBC    │ │ AS400 Conn││ │
│  └───────┬────────┘  └────────┬───────┘ └┬──────────┘ │
└──────────┼────────────────────┼───────────┼────────────┘
           │                    │           │
    ┌──────▼───────┐    ┌───────▼────────┐ ┌▼──────────┐
    │ Application  │    │ Oracle ERP     │ │ IBM AS400 │
    │ Database     │    │ GL_DAILY_RATES │ │ EXRATE    │
    │ (SQL Server) │    │ _INTERFACE     │ │ Table     │
    └──────────────┘    └────────────────┘ └───────────┘
```

## ⚙️ Scheduled Job Details

### Scraping Schedule
```java
@Scheduled(cron = "0 */10 16-21 * * *", zone = "Africa/Cairo")
```
- Runs **every 10 minutes** between **4:00 PM - 9:00 PM Cairo time**
- Checks if data already scraped today (avoids duplicates)
- If scraped, checks Oracle and AS400 status
- Pushes data if not already pushed
- Sends email notifications on success

### Email Alert Schedule
```java
@Scheduled(cron = "0 40 16 * * *", zone = "Africa/Cairo")
```
- Runs at **4:40 PM Cairo time** daily
- Sends failure alert if Oracle push hasn't happened yet

## 🗂️ Database Schema

### Application Database (ExchangeRate Entity)
```sql
CREATE TABLE exchange_rate (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    currency VARCHAR(50),
    banknote_buy VARCHAR(20),
    banknote_sell VARCHAR(20),
    transfer_buy VARCHAR(20),
    transfer_sell VARCHAR(20),
    scraped_at TIMESTAMP
);
```

### Oracle Integration
```sql
INSERT INTO apps.GL_DAILY_RATES_INTERFACE (
    FROM_CURRENCY,    -- e.g., 'USD'
    TO_CURRENCY,      -- Always 'EGP'
    FROM_CONVERSION_DATE,
    TO_CONVERSION_DATE,
    USER_CONVERSION_TYPE, -- 'Corporate'
    CONVERSION_RATE,
    USER_ID,          -- 1116
    MODE_FLAG         -- 'I' for Insert
)
```

### AS400 Integration
```sql
INSERT INTO ACCOUNT.EXRATE (
    EXYY,    -- Year (e.g., 2025)
    EXMM,    -- Month (e.g., 10)
    EXDD,    -- Day (e.g., 28)
    EXCUR,   -- Currency code (e.g., 'US', 'EU')
    EXAMT    -- Exchange rate
)
```

## 🎯 Currency Mapping

The system handles **18 currencies** with dual mapping:

| NBE Currency Name    | Oracle Code | AS400 Code |
|---------------------|-------------|------------|
| US DOLLAR           | USD         | US         |
| EURO                | EUR         | EU         |
| POUND STERLING      | GBP         | SL         |
| CANADIAN DOLLAR     | CAD         | CD         |
| YENS(100)           | JPY         | JY         |
| Kuwait DINAR        | KWD         | KD         |
| SAUDI RIALS         | SAR         | SR         |
| ... and 11 more

**Special Handling:**
- **YENS(100)**: Automatically divided by 100 during scraping
- **Transfer vs Banknote**: Prefers transfer sell rate, falls back to banknote sell

## 📧 Email Notifications

### Success Email
Sent when rates are successfully pushed:
- System name (Oracle/AS400)
- Timestamp
- HTML table with all pushed currencies and rates
- Recipients: Finance team + IT team (CC)

### Failure Alert
Sent if Oracle push hasn't happened by 4:40 PM:
- Red warning message
- Prompts finance team to contact IT

## 🧪 Testing

### Manual Testing Endpoints
1. **Test scraping**: `GET /api/rates/scrape`
2. **Check status**: `GET /api/rates/status/oracle`
3. **Test email**: `GET /api/rates/test-email`
4. **Backfill test**: `GET /api/rates/push/oracle/backfill?daysBack=3`

### Production Validation
```bash
# Check today's data
curl http://localhost:8080/api/rates/by-date?daysBack=0

# Verify Oracle status
curl http://localhost:8080/api/rates/status/oracle

# Verify AS400 status
curl http://localhost:8080/api/rates/status/as400
```

## 🐛 Troubleshooting

### Scraping Issues
- **Problem**: "No rates extracted"
    - **Solution**: NBE website structure may have changed. Check HTML selectors in service.

- **Problem**: WebDriver timeout
    - **Solution**: Increase timeout in `WebDriverWait` or check Edge browser installation.

### Database Issues
- **Problem**: "Duplicate key error"
    - **Solution**: The duplicate check should prevent this. Verify date logic.

- **Problem**: AS400 connection fails
    - **Solution**: Check AS400 connection details in `Utils.getAS400Connection()`.

### Email Issues
- **Problem**: Emails not sending
    - **Solution**: Verify SMTP configuration in `Utils.sendEmail()`.

## 🚀 Deployment

### Docker Deployment (Optional)
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/exchange-rates.war /app.war
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.war"]
```



## 👨‍💻 Author

**Your Name**
- GitHub: [@Mohamed-Mossa](https://github.com/Mohamed-Mossa)
- LinkedIn: [Mohamed Mousa](https://www.linkedin.com/in/mohamed-mousa-84010225a/)
- Email: MohamedMousa418@gmail.com

## 🙏 Acknowledgments

- National Bank of Egypt for providing exchange rate data
- Spring Boot team for excellent framework
- Selenium community for browser automation tools