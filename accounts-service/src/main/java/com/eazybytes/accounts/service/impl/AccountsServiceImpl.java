package com.kurobytes.accounts.service.impl;

import com.kurobytes.accounts.constants.AccountsConstants;
import com.kurobytes.accounts.dto.AccountsDto;
import com.kurobytes.accounts.dto.AccountsMsgDto;
import com.kurobytes.accounts.dto.CustomerDto;
import com.kurobytes.accounts.entity.Accounts;
import com.kurobytes.accounts.entity.Customer;
import com.kurobytes.accounts.exception.CustomerAlreadyExistsException;
import com.kurobytes.accounts.exception.ResourceNotFoundException;
import com.kurobytes.accounts.mapper.AccountsMapper;
import com.kurobytes.accounts.mapper.CustomerMapper;
import com.kurobytes.accounts.repository.AccountsRepository;
import com.kurobytes.accounts.repository.CustomerRepository;
import com.kurobytes.accounts.service.IAccountsService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Async;
import org.springframework.dao.DataIntegrityViolationException;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@AllArgsConstructor
@Timed(value = "business.operations") // Micrometerメトリクス
@Transactional(readOnly = true) // デフォルトは読み取り専用
public class AccountsServiceImpl  implements IAccountsService {

    private static final Logger log = LoggerFactory.getLogger(AccountsServiceImpl.class);

    private AccountsRepository accountsRepository;
    private CustomerRepository customerRepository;
    private final StreamBridge streamBridge;
    private final MeterRegistry meterRegistry;
    private final Counter accountCreationCounter;

    public AccountsServiceImpl(AccountsRepository accountsRepository,
                               CustomerRepository customerRepository,
                               StreamBridge streamBridge,
                               MeterRegistry meterRegistry) {
        this.accountsRepository = accountsRepository;
        this.customerRepository = customerRepository;
        this.streamBridge = streamBridge;
        this.meterRegistry = meterRegistry;
        this.accountCreationCounter = Counter.builder("accounts.created")
            .description("Number of accounts created")
            .register(meterRegistry);
    }

    /**
     * @param customerDto - CustomerDto Object
     */
    @Override
    @Transactional // 書き込み操作のみ明示的にTransactional
    public void createAccount(CustomerDto customerDto) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            Customer customer = CustomerMapper.mapToCustomer(customerDto, new Customer());
            Optional<Customer> optionalCustomer = customerRepository.findByMobileNumber(customerDto.getMobileNumber());
            if(optionalCustomer.isPresent()) {
                throw new CustomerAlreadyExistsException("Customer already registered with given mobileNumber "
                        +customerDto.getMobileNumber());
            }
            Customer savedCustomer = customerRepository.save(customer);
            Accounts savedAccount = accountsRepository.save(createNewAccount(savedCustomer));
            // 非同期通知（トランザクション外）
            sendCommunicationAsync(savedAccount, savedCustomer);
            // メトリクス記録
            accountCreationCounter.increment();
            meterRegistry.gauge("accounts.total.count", accountsRepository.count());
        } catch (DataIntegrityViolationException e) {
            meterRegistry.counter("accounts.creation.errors", "error.type", e.getClass().getSimpleName())
                .increment();
            log.error("Data integrity violation during account creation", e);
            throw new RuntimeException("Account creation failed due to data constraint", e);
        } finally {
            sample.stop(Timer.builder("accounts.creation.duration")
                .description("Account creation duration")
                .register(meterRegistry));
        }
    }

    @Async
    public void sendCommunicationAsync(Accounts account, Customer customer) {
        var accountsMsgDto = new AccountsMsgDto(account.getAccountNumber(), customer.getName(),
                customer.getEmail(), customer.getMobileNumber());
        log.info("Sending Communication request for the details: {}", accountsMsgDto);
        var result = streamBridge.send("sendCommunication-out-0", accountsMsgDto);
        log.info("Is the Communication request successfully triggered ? : {}", result);
    }

    /**
     * @param customer - Customer Object
     * @return the new account details
     */
    private Accounts createNewAccount(Customer customer) {
        Accounts newAccount = new Accounts();
        newAccount.setCustomerId(customer.getCustomerId());
        long randomAccNumber = 1000000000L + new Random().nextInt(900000000);

        newAccount.setAccountNumber(randomAccNumber);
        newAccount.setAccountType(AccountsConstants.SAVINGS);
        newAccount.setBranchAddress(AccountsConstants.ADDRESS);
        return newAccount;
    }

    /**
     * @param mobileNumber - Input Mobile Number
     * @return Accounts Details based on a given mobileNumber
     */
    @Override
    @Transactional(readOnly = true, timeout = 5) // 読み取り専用 + タイムアウト
    public CustomerDto fetchAccount(String mobileNumber) {
        Customer customer = customerRepository.findByMobileNumber(mobileNumber).orElseThrow(
                () -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber)
        );
        Accounts accounts = accountsRepository.findByCustomerId(customer.getCustomerId()).orElseThrow(
                () -> new ResourceNotFoundException("Account", "customerId", customer.getCustomerId().toString())
        );
        CustomerDto customerDto = CustomerMapper.mapToCustomerDto(customer, new CustomerDto());
        customerDto.setAccountsDto(AccountsMapper.mapToAccountsDto(accounts, new AccountsDto()));
        return customerDto;
    }

    /**
     * @param customerDto - CustomerDto Object
     * @return boolean indicating if the update of Account details is successful or not
     */
    @Override
    public boolean updateAccount(CustomerDto customerDto) {
        boolean isUpdated = false;
        AccountsDto accountsDto = customerDto.getAccountsDto();
        if(accountsDto !=null ){
            Accounts accounts = accountsRepository.findById(accountsDto.getAccountNumber()).orElseThrow(
                    () -> new ResourceNotFoundException("Account", "AccountNumber", accountsDto.getAccountNumber().toString())
            );
            AccountsMapper.mapToAccounts(accountsDto, accounts);
            accounts = accountsRepository.save(accounts);

            Long customerId = accounts.getCustomerId();
            Customer customer = customerRepository.findById(customerId).orElseThrow(
                    () -> new ResourceNotFoundException("Customer", "CustomerID", customerId.toString())
            );
            CustomerMapper.mapToCustomer(customerDto,customer);
            customerRepository.save(customer);
            isUpdated = true;
        }
        return  isUpdated;
    }

    /**
     * @param mobileNumber - Input Mobile Number
     * @return boolean indicating if the delete of Account details is successful or not
     */
    @Override
    public boolean deleteAccount(String mobileNumber) {
        Customer customer = customerRepository.findByMobileNumber(mobileNumber).orElseThrow(
                () -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber)
        );
        accountsRepository.deleteByCustomerId(customer.getCustomerId());
        customerRepository.deleteById(customer.getCustomerId());
        return true;
    }

    /**
     * @param accountNumber - Long
     * @return boolean indicating if the update of communication status is successful or not
     */
    @Override
    public boolean updateCommunicationStatus(Long accountNumber) {
        boolean isUpdated = false;
        if(accountNumber !=null ){
            Accounts accounts = accountsRepository.findById(accountNumber).orElseThrow(
                    () -> new ResourceNotFoundException("Account", "AccountNumber", accountNumber.toString())
            );
            accounts.setCommunicationSw(true);
            accountsRepository.save(accounts);
            isUpdated = true;
        }
        return  isUpdated;
    }


}
