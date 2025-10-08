package com.santander.pulse.application.controller;

import com.santander.pulse.domain.Customer;
import com.santander.pulse.infrastructure.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Customer Controller - Deletion Operations")
public class CustomerControllerDeleteTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
    }

    private Customer createValidCustomer() {
        Customer customer = new Customer("Joao Silva", "12345678901", "joao@email.com", "(11) 99999-9999");
        return customerRepository.save(customer);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should soft delete customer when customer exists and user is admin")
    void deleteCustomer_ShouldSoftDelete_WhenCustomerExistsAndUserIsAdmin() throws Exception {
        Customer customer = createValidCustomer();
        Long customerId = customer.getId();

        mockMvc.perform(delete("/customers/{id}", customerId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Customer deletedCustomer = customerRepository.findById(customerId).orElse(null);
        assert deletedCustomer != null;
        assert deletedCustomer.getStatus() == Customer.CustomerStatus.INATIVO;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return not found when customer does not exist")
    void deleteCustomer_ShouldReturnNotFound_WhenCustomerDoesNotExist() throws Exception {
        Long nonExistentId = 999L;

        mockMvc.perform(delete("/customers/{id}", nonExistentId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should allow deletion when user has any valid role")
    void deleteCustomer_ShouldAllowDeletion_WhenUserHasValidRole() throws Exception {
        Customer customer = createValidCustomer();
        Long customerId = customer.getId();

        mockMvc.perform(delete("/customers/{id}", customerId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Customer deletedCustomer = customerRepository.findById(customerId).orElse(null);
        assert deletedCustomer != null;
        assert deletedCustomer.getStatus() == Customer.CustomerStatus.INATIVO;
    }

    @Test
    @DisplayName("Should return unauthorized when user not authenticated")
    void deleteCustomer_ShouldReturnUnauthorized_WhenUserNotAuthenticated() throws Exception {
        Customer customer = createValidCustomer();
        Long customerId = customer.getId();

        mockMvc.perform(delete("/customers/{id}", customerId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
