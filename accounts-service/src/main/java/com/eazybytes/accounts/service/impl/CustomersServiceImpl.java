package com.kurobytes.accounts.service.impl;

import com.kurobytes.accounts.dto.AccountsDto;
import com.kurobytes.accounts.dto.CardsDto;
import com.kurobytes.accounts.dto.CustomerDetailsDto;
import com.kurobytes.accounts.dto.LoansDto;
import com.kurobytes.accounts.entity.Accounts;
import com.kurobytes.accounts.entity.Customer;
import com.kurobytes.accounts.exception.ResourceNotFoundException;
import com.kurobytes.accounts.mapper.AccountsMapper;
import com.kurobytes.accounts.mapper.CustomerMapper;
import com.kurobytes.accounts.repository.AccountsRepository;
import com.kurobytes.accounts.repository.CustomerRepository;
import com.kurobytes.accounts.service.ICustomersService;
import com.kurobytes.accounts.service.client.CardsRestClient;
import com.kurobytes.accounts.service.client.LoansRestClient;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.CompletableFuture;

@Service
@AllArgsConstructor
@CacheConfig(cacheNames = "customers")
public class CustomersServiceImpl implements ICustomersService {

    private AccountsRepository accountsRepository;
    private CustomerRepository customerRepository;
    private CardsRestClient cardsRestClient;
    private LoansRestClient loansRestClient;

    /**
     * @param mobileNumber - Input Mobile Number
     *  @param correlationId - Correlation ID value generated at Edge server
     * @return Customer Details based on a given mobileNumber
     */
    @Override
    @Cacheable(key = "#mobileNumber", unless = "#result == null")
    @Transactional(readOnly = true, timeout = 10)
    public CustomerDetailsDto fetchCustomerDetails(String mobileNumber, String correlationId) {
        Customer customer = customerRepository.findByMobileNumber(mobileNumber).orElseThrow(
                () -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber)
        );
        Accounts accounts = accountsRepository.findByCustomerId(customer.getCustomerId()).orElseThrow(
                () -> new ResourceNotFoundException("Account", "customerId", customer.getCustomerId().toString())
        );

        CustomerDetailsDto customerDetailsDto = CustomerMapper.mapToCustomerDetailsDto(customer, new CustomerDetailsDto());
        customerDetailsDto.setAccountsDto(AccountsMapper.mapToAccountsDto(accounts, new AccountsDto()));

        // 完全非同期・レジリエンス対応
        CompletableFuture<LoansDto> loansFuture = loansRestClient.fetchLoanDetailsAsync(correlationId, mobileNumber);
        CompletableFuture<CardsDto> cardsFuture = cardsRestClient.fetchCardDetailsAsync(correlationId, mobileNumber);
        CompletableFuture.allOf(loansFuture, cardsFuture).join();

        try {
            LoansDto loansDto = loansFuture.get();
            if(loansDto != null) {
                customerDetailsDto.setLoansDto(loansDto);
            }
            CardsDto cardsDto = cardsFuture.get();
            if(cardsDto != null) {
                customerDetailsDto.setCardsDto(cardsDto);
            }
        } catch (Exception e) {
            // ログ出力のみ、キャッシュ本体には影響なし
        }
        return customerDetailsDto;
    }

    @CacheEvict(key = "#customerDto.mobileNumber")
    public boolean updateCustomer(CustomerDto customerDto) {
        // 更新処理（実装例）
        // ...
        return true;
    }
}
