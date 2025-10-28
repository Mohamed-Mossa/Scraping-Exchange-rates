package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Table(name = "EXCHANGE_RATE")

public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "CURRENCY")
    private String currency;

    @Column(name = "TRANSFER_SELL")
    private String transferSell;

    @Column(name = "TRANSFER_BUY")
    private String transferBuy;

    @Column(name = "BANKNOTE_SELL")
    private String banknoteSell;

    @Column(name = "BANKNOTE_BUY")
    private String banknoteBuy;

    @Column(name = "SCRAPED_AT")
    private LocalDateTime scrapedAt;


}
