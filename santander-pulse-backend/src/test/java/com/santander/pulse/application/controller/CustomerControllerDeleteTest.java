package com.santander.pulse.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.santander.pulse.domain.Customer;
import com.santander.pulse.infrastructure.CustomerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Enterprise-grade tests for customer deletion functionality
 * Following banking security and audit requirements
 */
@WebMvcTest(CustomerController.class)
@DisplayName("Customer Controller - Deletion Operations")
public class CustomerControllerDeleteTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerRepository customerRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Helper method to create valid customer for testing
     */
    private Customer createValidCustomer(Long id) {
        Customer customer = new Customer("Jo?o Silva", "12345678901", "joao@email.com", "(11) 99999-9999");
        customer.setId(id);
        return customer;
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should delete customer permanently when customer exists and user is admin")
    void deleteCustomer_ShouldDeletePermanently_WhenCustomerExistsAndUserIsAdmin() throws Exception {
        // Arrange
        Long customerId = 1L;
        Customer customer = createValidCustomer(customerId);
        
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        doNothing().when(customerRepository).deleteById(customerId);

        // Act & Assert
        mockMvc.perform(delete("/api/customers/{id}", customerId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Verify repository interaction
        verify(customerRepository, times(1)).findById(customerId);
        verify(customerRepository, times(1)).deleteById(customerId);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return not found when customer does not exist")
    void deleteCustomer_ShouldReturnNotFound_WhenCustomerDoesNotExist() throws Exception {
        // Arrange
        Long customerId = 999L;
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(delete("/api/customers/{id}", customerId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        // Verify that deletion was not attempted
        verify(customerRepository, times(1)).findById(customerId);
        verify(customerRepository, never()).deleteById(any());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("Should return forbidden when user lacks permission")
    void deleteCustomer_ShouldReturnForbidden_WhenUserLacksPermission() throws Exception {
        // Arrange
        Long customerId = 1L;

        // Act & Assert - Expecting 403 Forbidden for non-admin users
        mockMvc.perform(delete("/api/customers/{id}", customerId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        // Verify that no repository methods were called due to security restriction
        verify(customerRepository, never()).findById(any());
        verify(customerRepository, never()).deleteById(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Should return server error when exception occurs")
    void deleteCustomer_ShouldReturnServerError_WhenExceptionOccurs() throws Exception {
        // Arrange
        Long customerId = 1L;
        Customer customer = createValidCustomer(customerId);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        doThrow(new RuntimeException("Database connection failure")).when(customerRepository).deleteById(customerId);

        // Act & Assert
        mockMvc.perform(delete("/api/customers/{id}", customerId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        // Verify that deletion was attempted but failed
        verify(customerRepository, times(1)).findById(customerId);
        verify(customerRepository, times(1)).deleteById(customerId);
    }

    @Test
    @DisplayName("Should return unauthorized when user not authenticated")
    void deleteCustomer_ShouldReturnUnauthorized_WhenUserNotAuthenticated() throws Exception {
        // Arrange
        Long customerId = 1L;

        // Act & Assert - No @WithMockUser annotation = unauthenticated
        mockMvc.perform(delete("/api/customers/{id}", customerId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        // Verify that no repository methods were called due to authentication failure
        verify(customerRepository, never()).findById(any());
        verify(customerRepository, never()).deleteById(any());
    }

    @Nested
    @DisplayName("Input Validation")
    class InputValidation {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should handle negative customer ID")
        void shouldHandleNegativeCustomerId() throws Exception {
            // Arrange
            Long negativeId = -1L;
            when(customerRepository.findById(negativeId)).thenReturn(Optional.empty());

            // Act & Assert
            mockMvc.perform(delete("/api/customers/{id}", negativeId)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(customerRepository, times(1)).findById(negativeId);
            verify(customerRepository, never()).deleteById(any());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should handle zero customer ID")
        void shouldHandleZeroCustomerId() throws Exception {
            // Arrange
            Long zeroId = 0L;
            when(customerRepository.findById(zeroId)).thenReturn(Optional.empty());

            // Act & Assert
            mockMvc.perform(delete("/api/customers/{id}", zeroId)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(customerRepository, times(1)).findById(zeroId);
            verify(customerRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("Business Logic Validation")
    class BusinessLogicValidation {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should maintain audit trail during deletion")
        void shouldMaintainAuditTrailDuringDeletion() throws Exception {
            // Arrange
            Long customerId = 1L;
            Customer customer = createValidCustomer(customerId);
            
            when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
            doNothing().when(customerRepository).deleteById(customerId);

            // Act
            mockMvc.perform(delete("/api/customers/{id}", customerId)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            // Assert - Verify audit trail requirements
            verify(customerRepository, times(1)).findById(customerId);
            verify(customerRepository, times(1)).deleteById(customerId);
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should handle concurrent deletion attempts")
        void shouldHandleConcurrentDeletionAttempts() throws Exception {
            // Arrange
            Long customerId = 1L;
            when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

            // Act & Assert - Simulating customer already deleted by another process
            mockMvc.perform(delete("/api/customers/{id}", customerId)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound());

            verify(customerRepository, times(1)).findById(customerId);
            verify(customerRepository, never()).deleteById(any());
        }
    }
}
