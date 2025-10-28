package com.example.demo.repository;

import com.example.demo.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    boolean existsByScrapedAtBetween(LocalDateTime start, LocalDateTime end);

    List<ExchangeRate> findByScrapedAtBetween(LocalDateTime start, LocalDateTime end); // ✅ ADD THIS
    // if you want by date only (ignoring time)
//    @Query("SELECT e FROM ExchangeRate e WHERE TRUNC(e.scrapedAt) = :date")
//    List<ExchangeRate> findByScrapedAtDate(@Param("date") LocalDate date);
}
