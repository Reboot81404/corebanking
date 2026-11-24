package com.bank.corebanking_prod.feature.customer.service;

import com.bank.corebanking_prod.feature.customer.model.dto.CustomerDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomerService {

    CustomerDto.Response createCustomer(CustomerDto.Create dto);

    CustomerDto.Response getCustomerById(Long id);

    CustomerDto.Response getCustomerByTckn(String tckn);

    Page<CustomerDto.Response> getAllCustomers(Pageable pageable);

    CustomerDto.Response updateCustomer(Long id, CustomerDto.Update dto);

    void deleteCustomer(Long id);

    boolean customerExists(Long id);

    boolean customerMatchesTckn(Long customerId, String tckn);

    Long findCustomerIdByTckn(String tckn);
}
