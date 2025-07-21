package com.kurobytes.accounts.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class AccountCreatedEvent {
    private String eventId;
    private Instant timestamp;
    private Long accountNumber;
    private String customerName;
    private String email;
    private String mobileNumber;
    private String eventType = "ACCOUNT_CREATED";
} 