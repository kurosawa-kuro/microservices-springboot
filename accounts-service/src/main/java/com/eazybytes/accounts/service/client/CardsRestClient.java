package com.kurobytes.accounts.service.client;

import com.kurobytes.accounts.dto.CardsDto;
import com.kurobytes.common.dto.ErrorResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.scheduling.annotation.Async;
import java.util.concurrent.CompletableFuture;
import org.springframework.web.client.ResourceAccessException;

@Component
public class CardsRestClient {

    private static final Logger log = LoggerFactory.getLogger(CardsRestClient.class);

    private final RestTemplate restTemplate;
    
    @Value("${microservices.cards.url:http://cards:9000}")
    private String cardsServiceUrl;

    public CardsRestClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ResponseEntity<CardsDto> fetchCardDetails(String correlationId, String mobileNumber) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("kurobank-correlation-id", correlationId);
        
        String url = UriComponentsBuilder.fromHttpUrl(cardsServiceUrl + "/api/fetch")
                .queryParam("mobileNumber", mobileNumber)
                .toUriString();
        
        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        try {
            return restTemplate.exchange(url, HttpMethod.GET, entity, CardsDto.class);
        } catch (Exception e) {
            log.error("Failed to fetch card details from {}: {}", url, e.getMessage(), e);
            ErrorResponseDto error = new ErrorResponseDto(
                url,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to fetch card details",
                LocalDateTime.now()
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null); // 実際のAPI設計に応じてErrorResponseDto返却も可
        }
    }

    @Async("taskExecutor")
    @Retry(name = "cards-service", maxAttempts = 3)
    @CircuitBreaker(name = "cards-service", fallbackMethod = "getDefaultCardsData")
    public CompletableFuture<CardsDto> fetchCardDetailsAsync(String correlationId, String mobileNumber) {
        try {
            ResponseEntity<CardsDto> response = fetchCardDetails(correlationId, mobileNumber);
            return CompletableFuture.completedFuture(response.getBody());
        } catch (Exception e) {
            log.warn("Failed to fetch cards for mobile: {}", mobileNumber, e);
            return CompletableFuture.completedFuture(null);
        }
    }

    public CompletableFuture<CardsDto> getDefaultCardsData(String correlationId, String mobileNumber, Throwable ex) {
        log.warn("Circuit breaker activated for cards service: {}", ex.getMessage());
        CardsDto defaultCards = new CardsDto(); // 必要に応じてデフォルト値をセット
        return CompletableFuture.completedFuture(defaultCards);
    }
}
