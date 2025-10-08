package com.santander.pulse.application.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.santander.pulse.infrastructure.CustomerRepository;

/**
 * Teste de integração para validar sincronização perfeita Frontend-Backend
 * na exclusão de clientes, seguindo arquitetura bancária enterprise.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Integração Frontend-Backend: Exclusão Sincronizada")
public class CustomerDeletionIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        customerRepository.deleteAll();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Fluxo completo: Criar → Visualizar → Excluir → Verificar sincronização")
    void testCompleteDeletionFlow() throws Exception {
        
        // 1. CRIAR CLIENTE
        String customerJson = """
            {
                "nome": "João Silva",
                "cpf": "12345678901",
                "email": "joao@teste.com",
                "telefone": "(11) 99999-9999"
            }
            """;
        
        MvcResult createResult = mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(customerJson))
                .andExpect(status().isCreated())
                .andReturn();
        
        Long customerId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();
        
        // 2. VERIFICAR PRESENÇA NA LISTA
        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.customers[0].nome").value("João Silva"))
                .andExpect(jsonPath("$.customers[0].status").value("ATIVO"));
        
        // 3. EXCLUIR COM CONTRATO ESTRUTURADO
        mockMvc.perform(delete("/customers/" + customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customerId))
                .andExpect(jsonPath("$.action").value("DEACTIVATED"))
                .andExpect(jsonPath("$.shouldRemoveFromList").value(true));
        
        // 4. VERIFICAR REMOÇÃO DA LISTA PADRÃO
        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        
        // 5. VERIFICAR PERSISTÊNCIA COM FILTRO
        mockMvc.perform(get("/customers?status=INATIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.customers[0].status").value("INATIVO"));
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Validação contrato de exclusão")
    void testDeletionContract() throws Exception {
        
        // Criar cliente para teste
        String customerJson = """
            {
                "nome": "Maria Santos",
                "cpf": "98765432100",
                "email": "maria@teste.com",
                "telefone": "(11) 88888-8888"
            }
            """;
        
        MvcResult createResult = mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(customerJson))
                .andExpect(status().isCreated())
                .andReturn();
        
        Long customerId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();
        
        // Testar contrato de exclusão
        MvcResult deleteResult = mockMvc.perform(delete("/customers/" + customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").exists())
                .andExpect(jsonPath("$.customerName").value("Maria Santos"))
                .andExpect(jsonPath("$.action").value("DEACTIVATED"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.shouldRemoveFromList").value(true))
                .andExpect(jsonPath("$.timestamp").exists())
                .andReturn();
        
        String response = deleteResult.getResponse().getContentAsString();
        JsonNode deleteResponse = objectMapper.readTree(response);
        
        // Validar estrutura completa da resposta
        assert deleteResponse.has("customerId");
        assert deleteResponse.has("customerName");
        assert deleteResponse.has("action");
        assert deleteResponse.has("message");
        assert deleteResponse.has("shouldRemoveFromList");
        assert deleteResponse.has("timestamp");
    }
}