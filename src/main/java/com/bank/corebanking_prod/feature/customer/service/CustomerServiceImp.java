package com.bank.corebanking_prod.feature.customer.service;

import com.bank.corebanking_prod.common.exception.DuplicateResourceException;
import com.bank.corebanking_prod.common.exception.ResourceNotFoundException;
import com.bank.corebanking_prod.feature.customer.model.dto.CustomerDto;
import com.bank.corebanking_prod.feature.customer.model.entity.CustomerEntity;
import com.bank.corebanking_prod.feature.customer.repository.CustomerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class CustomerServiceImp implements CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerServiceImp(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional
    public CustomerDto.Response createCustomer(CustomerDto.Create dto) {
        String normalizedEmail = normalizeEmail(dto.getEmail());

        if (customerRepository.existsByTckn(dto.getTckn())) {
            throw new DuplicateResourceException(
                    "A customer with this TCKN already exists: " + dto.getTckn()
            );
        }

        if (customerRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException(
                    "A customer with this email already exists: " + normalizedEmail
            );
        }

        CustomerEntity customerEntity = new CustomerEntity(
                dto.getTckn(),
                dto.getFirstName(),
                dto.getLastName(),
                normalizedEmail,
                dto.getPhoneNumber()
        );

        CustomerEntity savedEntity = customerRepository.save(customerEntity);
        return mapToResponseDto(savedEntity);
    }

    @Override
    public CustomerDto.Response getCustomerById(Long id) {
        CustomerEntity customerEntity = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with id: " + id
                ));

        return mapToResponseDto(customerEntity);
    }

    @Override
    public CustomerDto.Response getCustomerByTckn(String tckn) {
        CustomerEntity customerEntity = customerRepository.findByTckn(tckn)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer not found with TCKN: " + tckn
                ));

        return mapToResponseDto(customerEntity);
    }

    @Override
    public Page<CustomerDto.Response> getAllCustomers(Pageable pageable) {
        return customerRepository.findAll(pageable)
                .map(this::mapToResponseDto);
    }

    @Override
    @Transactional
    public CustomerDto.Response updateCustomer(Long id, CustomerDto.Update dto) {
        CustomerEntity existingEntity = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer to update was not found with id: " + id
                ));

        String normalizedEmail = normalizeEmail(dto.getEmail());
        String currentEmail = normalizeEmail(existingEntity.getEmail());

        if (!currentEmail.equals(normalizedEmail)
                && customerRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException(
                    "This email is already used by another customer: " + normalizedEmail
            );
        }

        existingEntity.setFirstName(dto.getFirstName());
        existingEntity.setLastName(dto.getLastName());
        existingEntity.setEmail(normalizedEmail);
        existingEntity.setPhoneNumber(dto.getPhoneNumber());

        CustomerEntity updatedEntity = customerRepository.save(existingEntity);
        return mapToResponseDto(updatedEntity);
    }

    @Override
    @Transactional
    public void deleteCustomer(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "Customer to delete was not found with id: " + id
            );
        }

        customerRepository.deleteById(id);
    }


    @Override
    public boolean customerExists(Long id) {
        return id != null && customerRepository.existsById(id);
    }

    @Override
    public boolean customerMatchesTckn(Long customerId, String tckn) {
        return customerId != null && tckn != null
                && customerRepository.findById(customerId)
                .map(customer -> tckn.equals(customer.getTckn()))
                .orElse(false);
    }

    @Override
    public Long findCustomerIdByTckn(String tckn) {
        if (tckn == null) {
            return null;
        }
        return customerRepository.findByTckn(tckn)
                .map(CustomerEntity::getId)
                .orElse(null);
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private CustomerDto.Response mapToResponseDto(CustomerEntity customerEntity) {
        return new CustomerDto.Response(
                customerEntity.getId(),
                customerEntity.getTckn(),
                customerEntity.getFirstName(),
                customerEntity.getLastName(),
                customerEntity.getEmail(),
                customerEntity.getPhoneNumber()
        );
    }
}
