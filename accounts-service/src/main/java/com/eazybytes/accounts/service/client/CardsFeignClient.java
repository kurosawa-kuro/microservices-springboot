package com.eazybytes.accounts.service.client;

import com.eazybytes.accounts.dto.CardsDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class CardsFeignClient {

    private final RestTemplate restTemplate;
    
    @Value("${microservices.cards.url:http://cards:9000}")
    private String cardsServiceUrl;

    public CardsFeignClient(RestTemplate restTemplate) {
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
            return ResponseEntity.notFound().build();
        }
    }

}
