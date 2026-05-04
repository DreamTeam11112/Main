package ru.stankin.antifraud.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionCheckRequest(
        @NotBlank @Size(max = 64) String transactionId,
        @NotBlank @Size(max = 64) String customerId,
        @NotBlank @Size(max = 64) String merchantId,
        @NotBlank @Size(max = 128) String cardToken,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotNull LocalDateTime timestamp,
        @NotBlank @Size(max = 45) String ipAddress,
        @Size(max = 128) String deviceId,
        @Pattern(regexp = "^[A-Z]{2}$", message = "country must be ISO-3166 alpha-2 code") String country,
        @Size(max = 128) String city,
        @NotBlank @Size(max = 32) String paymentMethod
) {
}
