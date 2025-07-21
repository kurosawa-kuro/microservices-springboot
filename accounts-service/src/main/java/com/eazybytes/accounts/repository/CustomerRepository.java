package com.kurobytes.accounts.repository;

import com.kurobytes.accounts.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByMobileNumber(String mobileNumber);

    @Query("SELECT c FROM Customer c LEFT JOIN FETCH c.accounts WHERE c.mobileNumber = :mobileNumber")
    Optional<Customer> findByMobileNumberWithAccounts(@Param("mobileNumber") String mobileNumber);

    @Query(value = "SELECT COUNT(*) FROM customer WHERE created_dt >= :startDate", nativeQuery = true)
    long countCustomersCreatedSince(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT c FROM Customer c WHERE c.name LIKE %:name%")
    Page<Customer> findByNameContaining(@Param("name") String name, Pageable pageable);

}
