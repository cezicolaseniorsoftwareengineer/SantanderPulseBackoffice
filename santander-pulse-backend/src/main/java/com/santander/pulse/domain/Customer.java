package com.santander.pulse.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Customer entity representing a banking customer.
 * Implements banking compliance validations for CPF and business rules.
 */
@Entity
@Table(name = "customers", indexes = {
    @Index(name = "idx_customer_cpf", columnList = "cpf"),
    @Index(name = "idx_customer_email", columnList = "email"),
    @Index(name = "idx_customer_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nome \u00e9 obrigat\u00f3rio")
    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @NotBlank(message = "CPF \u00e9 obrigat\u00f3rio")
    @Pattern(regexp = "\\d{11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}", 
             message = "CPF deve estar no formato 11111111111 ou 111.111.111-11")
    @Column(name = "cpf", unique = true, nullable = false, length = 14)
    private String cpf;

    @NotBlank(message = "Email \u00e9 obrigat\u00f3rio")
    @Email(message = "Email deve ser v\u00e1lido")
    @Column(name = "email", unique = true, nullable = false, length = 100)
    private String email;

    @NotBlank(message = "Telefone \u00e9 obrigat\u00f3rio")
    @Pattern(regexp = "\\(?\\d{2}\\)?[\\s-]?\\d{4,5}[\\s-]?\\d{4}", 
             message = "Telefone deve estar no formato (11) 99999-9999")
    @Column(name = "telefone", nullable = false, length = 20)
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CustomerStatus status = CustomerStatus.ATIVO;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Constructors
    public Customer() {}

    public Customer(String nome, String cpf, String email, String telefone) {
        this.nome = nome;
        this.setCpf(cpf);
        this.email = email;
        this.telefone = telefone;
        this.status = CustomerStatus.ATIVO;
    }

    // Business methods
    public void activate() {
        this.status = CustomerStatus.ATIVO;
    }

    public void deactivate() {
        this.status = CustomerStatus.INATIVO;
    }

    public void suspend() {
        this.status = CustomerStatus.SUSPENSO;
    }

    public boolean isActive() {
        return CustomerStatus.ATIVO.equals(this.status);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) {
        if (cpf == null) {
            this.cpf = null;
        } else {
            this.cpf = cpf.replaceAll("\\D", "");
        }
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public CustomerStatus getStatus() { return status; }
    public void setStatus(CustomerStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Customer customer = (Customer) o;
        return Objects.equals(id, customer.id) && Objects.equals(cpf, customer.cpf);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, cpf);
    }

    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", cpf='" + cpf.replaceAll("\\d(?=\\d{4})", "*") + '\'' +
                ", email='" + email + '\'' +
                ", status=" + status +
                '}';
    }

    /**
     * Customer status enumeration
     */
    public enum CustomerStatus {
        ATIVO, INATIVO, SUSPENSO
    }
}