package com.santander.pulse.application.controller;

import com.santander.pulse.application.dto.CustomerRequest;
import com.santander.pulse.application.dto.CustomerResponse;
import com.santander.pulse.domain.Customer;
import com.santander.pulse.infrastructure.CustomerRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Customer controller for CRUD operations with banking validations.
 * Implements JWT security and audit logging.
 */
@RestController
@RequestMapping("/customers")
@Tag(name = "Customers", description = "Customer management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class CustomerController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);

    private final CustomerRepository customerRepository;

    public CustomerController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @GetMapping
    @Operation(summary = "Get all customers", description = "Retrieve all customers with pagination")
    public ResponseEntity<Map<String, Object>> getAllCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "nome") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Customer.CustomerStatus status
    ) {
        try {
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                       Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<Customer> customerPage = customerRepository.findByCriteria(nome, email, status, pageable);
            
            List<CustomerResponse> customers = customerPage.getContent()
                .stream()
                .map(CustomerResponse::fromEntity)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("customers", customers);
            response.put("currentPage", customerPage.getNumber());
            response.put("totalElements", customerPage.getTotalElements());
            response.put("totalPages", customerPage.getTotalPages());
            response.put("pageSize", customerPage.getSize());

            logger.info("Retrieved {} customers (page {}/{})", 
                       customers.size(), page + 1, customerPage.getTotalPages());
            
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error retrieving customers: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Unable to retrieve customers"));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer by ID", description = "Retrieve a specific customer by ID")
    public ResponseEntity<?> getCustomerById(@PathVariable Long id) {
        try {
            Optional<Customer> customer = customerRepository.findById(id);
            
            if (customer.isPresent()) {
                CustomerResponse response = CustomerResponse.fromEntity(customer.get());
                logger.info("Retrieved customer with ID: {}", id);
                return ResponseEntity.ok(response);
            } else {
                logger.warn("Customer not found with ID: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Customer not found"));
            }

        } catch (Exception e) {
            logger.error("Error retrieving customer {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Unable to retrieve customer"));
        }
    }

    @PostMapping
    @Operation(summary = "Create customer", description = "Create a new customer with banking validations")
    public ResponseEntity<?> createCustomer(@Valid @RequestBody CustomerRequest customerRequest) {
        try {
            logger.info("Creating new customer: {}", customerRequest.nome());

            String normalizedCpf = customerRequest.cpf().replaceAll("\\D", "");

            // Check if CPF already exists
            if (customerRepository.existsByCpf(normalizedCpf)) {
                logger.warn("CPF already exists: {}", customerRequest.cpf());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "CPF already registered"));
            }

            // Check if email already exists
            if (customerRepository.existsByEmail(customerRequest.email())) {
                logger.warn("Email already exists: {}", customerRequest.email());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Email already registered"));
            }

            Customer customer = new Customer(
                customerRequest.nome(),
                normalizedCpf,
                customerRequest.email(),
                customerRequest.telefone()
            );
            
            // Set status if provided, otherwise defaults to ATIVO
            if (customerRequest.status() != null) {
                customer.setStatus(customerRequest.status());
            }

            Customer savedCustomer = customerRepository.save(customer);
            CustomerResponse response = CustomerResponse.fromEntity(savedCustomer);

            logger.info("Customer created successfully with ID: {}", savedCustomer.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.error("Error creating customer: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Unable to create customer"));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update customer", description = "Update an existing customer")
    public ResponseEntity<?> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequest customerRequest
    ) {
        try {
            Optional<Customer> existingCustomer = customerRepository.findById(id);
            
            if (!existingCustomer.isPresent()) {
                logger.warn("Customer not found for update with ID: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Customer not found"));
            }

            Customer customer = existingCustomer.get();
            
            // Check if email is being changed and if new email already exists
            if (!customer.getEmail().equals(customerRequest.email()) &&
                customerRepository.existsByEmail(customerRequest.email())) {
                logger.warn("Email already exists during update: {}", customerRequest.email());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Email already registered"));
            }

            customer.setNome(customerRequest.nome());
            customer.setEmail(customerRequest.email());
            customer.setTelefone(customerRequest.telefone());
            
            // Update status if provided (allows activation/deactivation of customers)
            if (customerRequest.status() != null) {
                customer.setStatus(customerRequest.status());
            }
            
            // Note: CPF should not be changed after creation for banking compliance

            Customer updatedCustomer = customerRepository.save(customer);
            CustomerResponse response = CustomerResponse.fromEntity(updatedCustomer);

            logger.info("Customer updated successfully with ID: {}", id);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error updating customer {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Unable to update customer"));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete customer", description = "Deactivate a customer (soft delete)")
    public ResponseEntity<?> deleteCustomer(@PathVariable Long id) {
        try {
            Optional<Customer> customerOpt = customerRepository.findById(id);
            
            if (!customerOpt.isPresent()) {
                logger.warn("Customer not found for deletion with ID: {}", id);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Customer not found"));
            }

            Customer customer = customerOpt.get();
            
            // Soft delete: deactivate customer instead of removing from database
            customer.deactivate();
            customerRepository.save(customer);

            logger.info("Customer deactivated (soft delete) with ID: {}", id);
            return ResponseEntity.ok(Map.of("message", "Customer deactivated successfully"));

        } catch (Exception e) {
            logger.error("Error deactivating customer {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Unable to delete customer"));
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Get customer statistics", description = "Get customer statistics dashboard")
    public ResponseEntity<Map<String, Object>> getCustomerStats() {
        try {
            long totalCustomers = customerRepository.count();
            long activeCustomers = customerRepository.countByStatus(Customer.CustomerStatus.ATIVO);
            long inactiveCustomers = customerRepository.countByStatus(Customer.CustomerStatus.INATIVO);
            
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            List<Customer> recentCustomers = customerRepository.findRecentCustomers(thirtyDaysAgo);

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalCustomers", totalCustomers);
            stats.put("activeCustomers", activeCustomers);
            stats.put("inactiveCustomers", inactiveCustomers);
            stats.put("recentCustomers", recentCustomers.size());
            stats.put("timestamp", LocalDateTime.now());

            logger.info("Retrieved customer statistics");
            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            logger.error("Error retrieving customer statistics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Unable to retrieve statistics"));
        }
    }
}