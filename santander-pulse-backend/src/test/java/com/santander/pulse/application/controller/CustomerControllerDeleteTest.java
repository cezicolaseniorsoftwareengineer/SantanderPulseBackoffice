package com.santander.pulse.application.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.santander.pulse.domain.Customer;
import com.santander.pulse.infrastructure.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Teste específico para funcionalidade de exclusão de clientes
 */
@WebMvcTest(CustomerController.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class CustomerControllerDeleteTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerRepository customerRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deleteCustomer_ShouldDeletePermanently_WhenCustomerExists() throws Exception {
        // Arrange
        Long customerId = 1L;
        Customer customer = new Customer("João Silva", "12345678901", "joao@email.com", "(11) 99999-9999");
        customer.setId(customerId);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        doNothing().when(customerRepository).deleteById(customerId);

        // Act & Assert
        mockMvc.perform(delete("/customers/{id}", customerId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Customer deleted successfully"));

        // Verify that deleteById was called instead of save
        verify(customerRepository, times(1)).findById(customerId);
        verify(customerRepository, times(1)).deleteById(customerId);
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void deleteCustomer_ShouldReturnNotFound_WhenCustomerDoesNotExist() throws Exception {
        // Arrange
        Long customerId = 999L;
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(delete("/customers/{id}", customerId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Customer not found"));

        // Verify that deleteById was not called
        verify(customerRepository, times(1)).findById(customerId);
        verify(customerRepository, never()).deleteById(any());
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void deleteCustomer_ShouldReturnServerError_WhenExceptionOccurs() throws Exception {
        // Arrange
        Long customerId = 1L;
        Customer customer = new Customer("João Silva", "12345678901", "joao@email.com", "(11) 99999-9999");
        customer.setId(customerId);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        doThrow(new RuntimeException("Database error")).when(customerRepository).deleteById(customerId);

        // Act & Assert
        mockMvc.perform(delete("/customers/{id}", customerId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Unable to delete customer"));

        // Verify that deleteById was called and threw exception
        verify(customerRepository, times(1)).findById(customerId);
        verify(customerRepository, times(1)).deleteById(customerId);
    }
}