package com.santander.pulse.application.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.santander.pulse.domain.Customer;
import com.santander.pulse.infrastructure.CustomerRepository;

/**
 * Uncle Bob, Kent Beck e Fowler Investigation Test
 * Reproduzindo EXATAMENTE o problema do cliente sumindo
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("🕵️ Uncle Bob Investigation: Where did the inactive customer go?")
public class CustomerInvestigationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        customerRepository.deleteAll();

        // Uncle Bob: "Make the test obvious"
        testCustomer = new Customer("CLIENTE TESTE", "12345678901", "teste@cliente.com", "(11) 99999-9999");
        testCustomer.setStatus(Customer.CustomerStatus.ATIVO);
        testCustomer = customerRepository.save(testCustomer);
        
        System.out.println("🔍 [UNCLE BOB] Cliente criado com ID: " + testCustomer.getId() + ", Status: " + testCustomer.getStatus());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("🔍 Uncle Bob: Step 1 - Cliente ativo aparece no dashboard")
    void step1_ClienteAtivoApareceNoDashboard() throws Exception {
        System.out.println("🔍 [UNCLE BOB] Testando se cliente ATIVO aparece...");
        
        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.customers[0].nome").value("CLIENTE TESTE"))
                .andExpect(jsonPath("$.customers[0].status").value("ATIVO"));
        
        System.out.println("✅ [UNCLE BOB] Step 1 OK: Cliente ativo aparece corretamente");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("🔍 Kent Beck: Step 2 - Soft delete muda status para INATIVO")
    void step2_SoftDeleteMudaStatusParaInativo() throws Exception {
        System.out.println("🔍 [KENT BECK] Fazendo soft delete do cliente...");
        
        // Fazer soft delete
        mockMvc.perform(delete("/customers/" + testCustomer.getId()))
                .andExpect(status().isOk());
        
        // Verificar se cliente ainda existe mas com status INATIVO
        Customer deletedCustomer = customerRepository.findById(testCustomer.getId()).orElse(null);
        System.out.println("🔍 [KENT BECK] Cliente após delete - ID: " + deletedCustomer.getId() + ", Status: " + deletedCustomer.getStatus());
        
        assert deletedCustomer != null;
        assert deletedCustomer.getStatus() == Customer.CustomerStatus.INATIVO;
        
        System.out.println("✅ [KENT BECK] Step 2 OK: Soft delete funcionou corretamente");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("🔍 Fowler: Step 3 - Dashboard default NÃO deve mostrar cliente inativo")
    void step3_DashboardDefaultNaoMostraClienteInativo() throws Exception {
        System.out.println("🔍 [FOWLER] Testando dashboard após soft delete...");
        
        // Fazer soft delete primeiro
        mockMvc.perform(delete("/customers/" + testCustomer.getId()))
                .andExpect(status().isOk());
        
        // Dashboard default (sem filtro) NÃO deve mostrar cliente inativo
        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        
        System.out.println("✅ [FOWLER] Step 3 OK: Dashboard não mostra cliente inativo");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("🔍 TDD: Step 4 - Filtro status=INATIVO DEVE mostrar cliente inativo")
    void step4_FiltroInativoDeveMostrarClienteInativo() throws Exception {
        System.out.println("🔍 [TDD] Testando filtro específico para inativos...");
        
        // Fazer soft delete primeiro
        mockMvc.perform(delete("/customers/" + testCustomer.getId()))
                .andExpect(status().isOk());
        
        // Filtro status=INATIVO DEVE mostrar o cliente
        mockMvc.perform(get("/customers?status=INATIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.customers[0].nome").value("CLIENTE TESTE"))
                .andExpect(jsonPath("$.customers[0].status").value("INATIVO"));
        
        System.out.println("✅ [TDD] Step 4 OK: Filtro mostra cliente inativo corretamente");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("🚨 PROBLEMA REAL: O que acontece quando você marca como inativo manualmente?")
    void step5_ProblemaReal_MarcarComoInativoManualmente() throws Exception {
        System.out.println("🚨 [PROBLEMA REAL] Simulando marcar cliente como inativo manualmente...");
        
        // Simular marcar como inativo (não através do delete, mas diretamente)
        testCustomer.setStatus(Customer.CustomerStatus.INATIVO);
        customerRepository.save(testCustomer);
        
        System.out.println("🔍 Cliente marcado como INATIVO manualmente");
        System.out.println("🔍 Status no banco: " + customerRepository.findById(testCustomer.getId()).get().getStatus());
        
        // Verificar se aparece no dashboard default (NÃO deveria aparecer)
        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        
        // Verificar se aparece com filtro INATIVO (DEVERIA aparecer)
        mockMvc.perform(get("/customers?status=INATIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.customers[0].status").value("INATIVO"));
        
        System.out.println("✅ [INVESTIGAÇÃO] Cliente inativo funciona corretamente!");
        System.out.println("🎯 [CONCLUSÃO] O problema pode estar em como você está buscando os dados no frontend!");
    }
}