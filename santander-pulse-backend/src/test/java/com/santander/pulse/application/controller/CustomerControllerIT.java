package com.santander.pulse.application.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.santander.pulse.domain.Customer;
import com.santander.pulse.infrastructure.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "admin@santander.com", roles = {"ADMIN"})
class CustomerControllerIT {

    private static final String CONTEXT_PATH = "/api";
    private static final String CUSTOMERS_ENDPOINT = "/customers";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @BeforeEach
    void cleanDatabase() {
        customerRepository.deleteAll();
    }

    @Nested
    @DisplayName("Customer creation")
    class CreateCustomer {

        @Test
        @DisplayName("should create a customer when payload is valid and CPF is unique")
        void shouldCreateCustomer() throws Exception {
            String payload = objectMapper.createObjectNode()
                .put("nome", "Ana Clara Souza")
                .put("cpf", "350.602.688-71")
                .put("email", "ana.souza@santander.com")
                .put("telefone", "(11) 93333-4444")
                .put("status", "ATIVO")
                .toString();

            var result = mockMvc.perform(post(CONTEXT_PATH + CUSTOMERS_ENDPOINT)
                    .contextPath(CONTEXT_PATH)
                    .servletPath(CUSTOMERS_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Ana Clara Souza"))
                .andExpect(jsonPath("$.cpf").value("35060268871"))
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andReturn();

            JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
            Long customerId = response.get("id").asLong();

            Customer saved = customerRepository.findById(customerId).orElseThrow();
            assertThat(saved.getCpf()).isEqualTo("35060268871");
            assertThat(saved.getEmail()).isEqualTo("ana.souza@santander.com");
            assertThat(saved.getStatus()).isEqualTo(Customer.CustomerStatus.ATIVO);
        }
    }

    @Nested
    @DisplayName("Customer update")
    class UpdateCustomer {

        @Test
        @DisplayName("should update editable fields and keep CPF immutable")
        void shouldUpdateCustomer() throws Exception {
            Customer existing = customerRepository.save(new Customer(
                "Bruno Almeida",
                "12345678901",
                "bruno.almeida@santander.com",
                "(11) 95555-6666"
            ));

            String payload = objectMapper.createObjectNode()
                .put("nome", "Bruno A. Lima")
                .put("cpf", "12345678901")
                .put("email", "bruno.lima@santander.com")
                .put("telefone", "(11) 97777-8888")
                .put("status", "INATIVO")
                .toString();

            mockMvc.perform(put(CONTEXT_PATH + CUSTOMERS_ENDPOINT + "/" + existing.getId())
                    .contextPath(CONTEXT_PATH)
                    .servletPath(CUSTOMERS_ENDPOINT + "/" + existing.getId())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Bruno A. Lima"))
                .andExpect(jsonPath("$.email").value("bruno.lima@santander.com"))
                .andExpect(jsonPath("$.telefone").value("(11) 97777-8888"))
                .andExpect(jsonPath("$.status").value("INATIVO"))
                .andExpect(jsonPath("$.cpf").value("12345678901"));

            Customer updated = customerRepository.findById(existing.getId()).orElseThrow();
            assertThat(updated.getNome()).isEqualTo("Bruno A. Lima");
            assertThat(updated.getEmail()).isEqualTo("bruno.lima@santander.com");
            assertThat(updated.getTelefone()).isEqualTo("(11) 97777-8888");
            assertThat(updated.getStatus()).isEqualTo(Customer.CustomerStatus.INATIVO);
            assertThat(updated.getCpf()).isEqualTo("12345678901");
        }
    }

    @Nested
    @DisplayName("Customer deletion")
    class DeleteCustomer {

        @Test
        @DisplayName("should soft delete customer by setting status to INATIVO")
        void shouldSoftDeleteCustomer() throws Exception {
            Customer activeCustomer = customerRepository.save(new Customer(
                "Carla Monteiro",
                "55566677788",
                "carla.monteiro@santander.com",
                "(11) 98888-9999"
            ));

            mockMvc.perform(delete(CONTEXT_PATH + CUSTOMERS_ENDPOINT + "/" + activeCustomer.getId())
                    .contextPath(CONTEXT_PATH)
                    .servletPath(CUSTOMERS_ENDPOINT + "/" + activeCustomer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Customer deactivated successfully"));

            Customer deleted = customerRepository.findById(activeCustomer.getId()).orElseThrow();
            assertThat(deleted.getStatus()).isEqualTo(Customer.CustomerStatus.INATIVO);
        }
    }
}