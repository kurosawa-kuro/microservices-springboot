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

@Service
@AllArgsConstructor
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
    public CustomerDetailsDto fetchCustomerDetails(String mobileNumber, String correlationId) {
        Customer customer = customerRepository.findByMobileNumber(mobileNumber).orElseThrow(
                () -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber)
        );
        Accounts accounts = accountsRepository.findByCustomerId(customer.getCustomerId()).orElseThrow(
                () -> new ResourceNotFoundException("Account", "customerId", customer.getCustomerId().toString())
        );

        CustomerDetailsDto customerDetailsDto = CustomerMapper.mapToCustomerDetailsDto(customer, new CustomerDetailsDto());
        customerDetailsDto.setAccountsDto(AccountsMapper.mapToAccountsDto(accounts, new AccountsDto()));

        ResponseEntity<LoansDto> loansDtoResponseEntity = loansRestClient.fetchLoanDetails(correlationId, mobileNumber);
        if(null != loansDtoResponseEntity) {
            customerDetailsDto.setLoansDto(loansDtoResponseEntity.getBody());
        }

        ResponseEntity<CardsDto> cardsDtoResponseEntity = cardsRestClient.fetchCardDetails(correlationId, mobileNumber);
        if(null != cardsDtoResponseEntity) {
            customerDetailsDto.setCardsDto(cardsDtoResponseEntity.getBody());
        }


        return customerDetailsDto;

    }
}
