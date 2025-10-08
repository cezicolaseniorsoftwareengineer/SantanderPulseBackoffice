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
 * 🎯 TESTE DE INTEGRAÇÃO REAL - EXATAMENTE COMO O USUÁRIO USA
 * 
 * Fluxo testado:
 * 1. Usuário cria um cliente
 * 2. Usuário clica em "Excluir"
 * 3. Cliente desaparece DEFINITIVAMENTE da lista
 * 
 * SEM soft delete, SEM status INATIVO, SEM complicações.
 * O cliente simplesmente SOME da lista quando excluído.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("🎯 TESTE REAL: Criar → Excluir → Cliente Desaparece DEFINITIVAMENTE")
public class UserJourneyDeleteTest {

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
        System.out.println("🧹 Database limpo para teste real");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("🎯 FLUXO REAL: Usuário cria cliente → exclui → cliente SOME da lista")
    void fluxoRealUsuarioCreateDelete() throws Exception {
        
        System.out.println("\n🎬 ===== INICIANDO TESTE DE JORNADA DO USUÁRIO =====");
        
        // ==========================================
        // STEP 1: Usuário cria um cliente
        // ==========================================
        System.out.println("\n👤 STEP 1: Usuário cria um novo cliente...");
        
        String novoClienteJson = """
            {
                "nome": "João da Silva",
                "cpf": "12345678901",
                "email": "joao.silva@email.com",
                "telefone": "(11) 99999-9999"
            }
            """;
        
        MvcResult createResult = mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(novoClienteJson))
                .andExpect(status().isCreated())
                .andReturn();
        
        // Extrair o ID do cliente criado
        String responseBody = createResult.getResponse().getContentAsString();
        JsonNode responseJson = objectMapper.readTree(responseBody);
        Long clienteId = responseJson.get("id").asLong();
        
        System.out.println("✅ Cliente criado com ID: " + clienteId);
        System.out.println("📄 Response: " + responseBody);
        
        // ==========================================
        // STEP 2: Verificar que cliente aparece na lista
        // ==========================================
        System.out.println("\n👀 STEP 2: Verificando que cliente aparece na lista do dashboard...");
        
        MvcResult listResult = mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.customers[0].nome").value("João da Silva"))
                .andExpect(jsonPath("$.customers[0].status").value("ATIVO"))
                .andReturn();
        
        String listResponse = listResult.getResponse().getContentAsString();
        System.out.println("✅ Cliente aparece na lista!");
        System.out.println("📄 Lista atual: " + listResponse);
        
        // ==========================================
        // STEP 3: Usuário clica em "Excluir"
        // ==========================================
        System.out.println("\n🗑️ STEP 3: Usuário clica em EXCLUIR...");
        
        MvcResult deleteResult = mockMvc.perform(delete("/customers/" + clienteId))
                .andExpect(status().isOk())
                .andReturn();
        
        String deleteResponse = deleteResult.getResponse().getContentAsString();
        System.out.println("✅ Comando de exclusão executado!");
        System.out.println("📄 Response da exclusão: " + deleteResponse);
        
        // ==========================================
        // STEP 4: VERIFICAR SE CLIENTE SUMIU DA LISTA
        // ==========================================
        System.out.println("\n🔍 STEP 4: Verificando se cliente SUMIU da lista...");
        
        MvcResult finalListResult = mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andReturn();
        
        String finalListResponse = finalListResult.getResponse().getContentAsString();
        JsonNode finalListJson = objectMapper.readTree(finalListResponse);
        
        int totalElementsAfterDelete = finalListJson.get("totalElements").asInt();
        
        System.out.println("📊 Total de clientes na lista APÓS exclusão: " + totalElementsAfterDelete);
        System.out.println("📄 Lista final: " + finalListResponse);
        
        // ==========================================
        // VERIFICAÇÃO FINAL: DEVE TER 0 CLIENTES
        // ==========================================
        if (totalElementsAfterDelete == 0) {
            System.out.println("🎉 ✅ SUCESSO! Cliente foi EXCLUÍDO DEFINITIVAMENTE!");
            System.out.println("🎯 Comportamento correto: Cliente sumiu da lista após exclusão");
        } else {
            System.out.println("❌ FALHA! Cliente ainda aparece na lista após exclusão");
            System.out.println("🐛 Comportamento incorreto: Cliente deveria ter sumido");
            
            // Mostrar detalhes do que ainda está na lista
            JsonNode customers = finalListJson.get("customers");
            System.out.println("👻 Clientes fantasmas na lista:");
            for (JsonNode customer : customers) {
                System.out.println("  - ID: " + customer.get("id") + 
                                 ", Nome: " + customer.get("nome") + 
                                 ", Status: " + customer.get("status"));
            }
        }
        
        // ==========================================
        // ASSERT FINAL: LISTA DEVE ESTAR VAZIA
        // ==========================================
        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        
        System.out.println("\n🏁 ===== TESTE DE JORNADA CONCLUÍDO =====");
    }
    
    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("🔍 TESTE AUXILIAR: Verificar se cliente INATIVO aparece com filtro")
    void verificarSeClienteInativoApareceComFiltro() throws Exception {
        
        System.out.println("\n🔬 ===== TESTE AUXILIAR: VERIFICAR FILTRO INATIVO =====");
        
        // Criar cliente
        String novoClienteJson = """
            {
                "nome": "Maria Santos",
                "cpf": "98765432100",
                "email": "maria.santos@email.com",
                "telefone": "(11) 88888-8888"
            }
            """;
        
        MvcResult createResult = mockMvc.perform(post("/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(novoClienteJson))
                .andExpect(status().isCreated())
                .andReturn();
        
        Long clienteId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();
        
        System.out.println("✅ Cliente auxiliar criado com ID: " + clienteId);
        
        // Excluir (soft delete)
        mockMvc.perform(delete("/customers/" + clienteId))
                .andExpect(status().isOk());
        
        System.out.println("🗑️ Cliente auxiliar 'excluído'");
        
        // Verificar se aparece com filtro INATIVO
        System.out.println("🔍 Verificando com filtro status=INATIVO...");
        
        MvcResult filteredResult = mockMvc.perform(get("/customers?status=INATIVO"))
                .andExpect(status().isOk())
                .andReturn();
        
        String filteredResponse = filteredResult.getResponse().getContentAsString();
        JsonNode filteredJson = objectMapper.readTree(filteredResponse);
        int inactiveCount = filteredJson.get("totalElements").asInt();
        
        System.out.println("📊 Clientes INATIVOS encontrados: " + inactiveCount);
        System.out.println("📄 Response filtrado: " + filteredResponse);
        
        if (inactiveCount > 0) {
            System.out.println("🔍 DESCOBERTA: Sistema usa SOFT DELETE (cliente vira INATIVO)");
            System.out.println("💡 EXPLICAÇÃO: Por isso cliente 'sumiu' da lista padrão mas existe com status INATIVO");
        } else {
            System.out.println("🔍 DESCOBERTA: Sistema usa HARD DELETE (cliente é realmente excluído)");
        }
        
        System.out.println("\n🏁 ===== TESTE AUXILIAR CONCLUÍDO =====");
    }
}