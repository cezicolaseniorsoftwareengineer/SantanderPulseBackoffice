package com.santander.pulse.application.dto;

import com.santander.pulse.domain.Customer;

import java.time.LocalDateTime;

/**
 * DTO for customer response
 */
public class CustomerResponse {

    private Long id;
    private String nome;
    private String cpf;
    private String email;
    private String telefone;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    public CustomerResponse() {}

    public CustomerResponse(Customer customer) {
        this.id = customer.getId();
        this.nome = customer.getNome();
        this.cpf = customer.getCpf();
        this.email = customer.getEmail();
        this.telefone = customer.getTelefone();
        this.status = customer.getStatus().name();
        this.createdAt = customer.getCreatedAt();
        this.updatedAt = customer.getUpdatedAt();
    }

    /**
     * Factory method to create CustomerResponse from Customer entity
     */
    public static CustomerResponse fromEntity(Customer customer) {
        return new CustomerResponse(customer);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "CustomerResponse{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", cpf='" + (cpf != null ? cpf.replaceAll("\\d(?=\\d{4})", "*") : null) + '\'' +
                ", email='" + email + '\'' +
                ", telefone='" + telefone + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}