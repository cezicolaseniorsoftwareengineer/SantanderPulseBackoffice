package com.santander.pulse.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import com.santander.pulse.domain.Customer;

public record CustomerRequest(
    @NotBlank(message = "Nome e obrigatorio")
    String nome,
    
    @NotBlank(message = "CPF e obrigatorio")
    @Pattern(
        regexp = "\\d{11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}",
        message = "CPF deve estar no formato 11111111111 ou 111.111.111-11"
    )
    String cpf,
    
    @NotBlank(message = "Email e obrigatorio")
    @Email(message = "Email deve ser valido")
    String email,
    
    @NotBlank(message = "Telefone e obrigatorio")
    @Pattern(regexp = "\\(?\\d{2}\\)?[\\s-]?\\d{4,5}[\\s-]?\\d{4}", message = "Telefone deve estar no formato (11) 99999-9999")
    String telefone,
    
    Customer.CustomerStatus status
) {}