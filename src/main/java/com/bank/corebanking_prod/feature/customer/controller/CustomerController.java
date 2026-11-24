package com.bank.corebanking_prod.feature.customer.controller;

import com.bank.corebanking_prod.feature.customer.model.dto.CustomerDto;
import com.bank.corebanking_prod.feature.customer.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('BANK_OPERATOR', 'ADMIN')")
    public ResponseEntity<CustomerDto.Response> createCustomer(
            @Valid @RequestBody CustomerDto.Create dto) {
        CustomerDto.Response response = customerService.createCustomer(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@resourceOwnershipService.canAccessCustomer(#id)")
    public ResponseEntity<CustomerDto.Response> getCustomerById(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerById(id));
    }

    @GetMapping("/tckn/{tckn}")
    @PreAuthorize("@resourceOwnershipService.canAccessCustomerByTckn(#tckn)")
    public ResponseEntity<CustomerDto.Response> getCustomerByTckn(@PathVariable String tckn) {
        return ResponseEntity.ok(customerService.getCustomerByTckn(tckn));
    }

    @GetMapping
    @PreAuthorize("@resourceOwnershipService.canListCustomers()")
    public ResponseEntity<Page<CustomerDto.Response>> getAllCustomers(
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "id",
                    direction = Sort.Direction.ASC
            ) Pageable pageable) {
        return ResponseEntity.ok(customerService.getAllCustomers(pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('BANK_OPERATOR', 'ADMIN')")
    public ResponseEntity<CustomerDto.Response> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerDto.Update dto) {
        return ResponseEntity.ok(customerService.updateCustomer(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}
