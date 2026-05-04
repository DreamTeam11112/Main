package ru.stankin.antifraud.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "customer_profiles")
public class CustomerProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, unique = true, length = 64)
    private String customerId;

    @Column(name = "avg_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal avgAmount;

    @Column(name = "tx_count_24h", nullable = false)
    private Integer txCount24h;

    @Column(name = "last_country", length = 2)
    private String lastCountry;

    @Column(name = "last_city", length = 128)
    private String lastCity;

    @Column(name = "last_device_id", length = 128)
    private String lastDeviceId;

    @Column(name = "last_transaction_time")
    private LocalDateTime lastTransactionTime;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getAvgAmount() {
        return avgAmount;
    }

    public void setAvgAmount(BigDecimal avgAmount) {
        this.avgAmount = avgAmount;
    }

    public Integer getTxCount24h() {
        return txCount24h;
    }

    public void setTxCount24h(Integer txCount24h) {
        this.txCount24h = txCount24h;
    }

    public String getLastCountry() {
        return lastCountry;
    }

    public void setLastCountry(String lastCountry) {
        this.lastCountry = lastCountry;
    }

    public String getLastCity() {
        return lastCity;
    }

    public void setLastCity(String lastCity) {
        this.lastCity = lastCity;
    }

    public String getLastDeviceId() {
        return lastDeviceId;
    }

    public void setLastDeviceId(String lastDeviceId) {
        this.lastDeviceId = lastDeviceId;
    }

    public LocalDateTime getLastTransactionTime() {
        return lastTransactionTime;
    }

    public void setLastTransactionTime(LocalDateTime lastTransactionTime) {
        this.lastTransactionTime = lastTransactionTime;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
