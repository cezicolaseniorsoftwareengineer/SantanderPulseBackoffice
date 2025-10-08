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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.santander.pulse.infrastructure.CustomerRepository;

/**
 * Teste para demonstrar a solução do problema "cliente inativo desapareceu"
 * Mostra como usar os filtros de status para encontrar clientes inativos
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Solução: Cliente Inativo Desaparecido")
public class CustomerStatusFilterTest {

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
    @DisplayName("Problema resolvido: Como encontrar cliente que 'desapareceu'")
    void testFindMissingInactiveCustomer() throws Exception {
        
        System.out.println("\n🔍 === DEMONSTRAÇÃO: CLIENTE QUE 'DESAPARECEU' ===");
        
        // 1. CRIAR CLIENTE
        String customerJson = """
            {
                "nome": "Cliente Que Vai Sumir",
                "cpf": "11111111111",
                "email": "sumir@teste.com",
                "telefone": "(11) 11111-1111"
            }
            """;
        
        MvcResult createResult = mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(customerJson))
                .andExpect(status().isCreated())
                .andReturn();
        
        Long customerId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();
                
        System.out.println("✅ Step 1: Cliente criado com ID: " + customerId);
        
        // 2. VERIFICAR QUE CLIENTE APARECE NA LISTA PADRÃO (apenas ativos)
        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.customers[0].status").value("ATIVO"));
                
        System.out.println("✅ Step 2: Cliente aparece na lista padrão (ATIVO)");
        
        // 3. "EXCLUIR" O CLIENTE (soft delete = muda para INATIVO)
        mockMvc.perform(delete("/customers/" + customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.action").value("DEACTIVATED"));
                
        System.out.println("⚠️ Step 3: Cliente 'excluído' (virou INATIVO)");
        
        // 4. PROBLEMA: Cliente "desapareceu" da lista padrão
        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
                
        System.out.println("❌ Step 4: PROBLEMA - Cliente 'desapareceu' da lista!");
        
        // 5. SOLUÇÃO: Usar filtro status=INATIVO para encontrar
        mockMvc.perform(get("/customers?status=INATIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.customers[0].status").value("INATIVO"));
                
        System.out.println("✅ Step 5: SOLUÇÃO - Cliente encontrado com filtro INATIVO!");
        
        // 6. VERIFICAÇÃO: Mostrar TODOS os clientes (sem filtro)
        // Nota: Como status=null, backend mostra apenas ATIVOS por padrão
        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
                
        System.out.println("📋 Step 6: Lista padrão mostra apenas ATIVOS (0 clientes)");
        
        System.out.println("\n🎯 === RESUMO DA SOLUÇÃO ===");
        System.out.println("❓ PROBLEMA: Cliente 'desapareceu' após marcar como inativo");
        System.out.println("✅ SOLUÇÃO: Sistema usa SOFT DELETE - cliente vira INATIVO");
        System.out.println("🔍 FILTROS DISPONÍVEIS:");
        System.out.println("   • /customers (padrão) → Apenas ATIVOS");
        System.out.println("   • /customers?status=ATIVO → Apenas ATIVOS");
        System.out.println("   • /customers?status=INATIVO → Apenas INATIVOS");
        System.out.println("📱 FRONTEND: Use o dropdown de filtro para alternar entre eles");
        System.out.println("=====================================\n");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Demonstração: Todos os filtros de status")
    void testAllStatusFilters() throws Exception {
        
        System.out.println("\n📊 === DEMONSTRAÇÃO: TODOS OS FILTROS ===");
        
        // Criar 3 clientes para teste
        String[] customers = {
            """
            {
                "nome": "Cliente Ativo 1",
                "cpf": "22222222222",
                "email": "ativo1@teste.com",
                "telefone": "(11) 22222-2222"
            }
            """,
            """
            {
                "nome": "Cliente Ativo 2", 
                "cpf": "33333333333",
                "email": "ativo2@teste.com",
                "telefone": "(11) 33333-3333"
            }
            """,
            """
            {
                "nome": "Cliente Para Inativar",
                "cpf": "44444444444", 
                "email": "inativar@teste.com",
                "telefone": "(11) 44444-4444"
            }
            """
        };
        
        Long[] customerIds = new Long[3];
        
        // Criar todos os clientes
        for (int i = 0; i < customers.length; i++) {
            MvcResult result = mockMvc.perform(post("/customers")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(customers[i]))
                    .andExpect(status().isCreated())
                    .andReturn();
                    
            customerIds[i] = objectMapper.readTree(result.getResponse().getContentAsString())
                    .get("id").asLong();
        }
        
        // Inativar o terceiro cliente
        mockMvc.perform(delete("/customers/" + customerIds[2]))
                .andExpect(status().isOk());
                
        System.out.println("✅ Criados: 2 ativos + 1 inativo");
        
        // Testar filtro padrão (apenas ativos)
        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
                
        System.out.println("🔍 Filtro padrão: 2 clientes (apenas ATIVOS)");
        
        // Testar filtro explícito para ativos
        mockMvc.perform(get("/customers?status=ATIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
                
        System.out.println("🔍 Filtro ATIVO: 2 clientes");
        
        // Testar filtro para inativos
        mockMvc.perform(get("/customers?status=INATIVO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
                
        System.out.println("🔍 Filtro INATIVO: 1 cliente");
        
        System.out.println("=====================================\n");
    }
}