package com.santander.pulse.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO for login request
 */
public class LoginRequest {

    @NotBlank(message = "CPF is required")
    @Pattern(regexp = "\\d{11}", message = "CPF must contain exactly 11 digits")
    private String cpf;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    // Constructors
    public LoginRequest() {}

    public LoginRequest(String cpf, String password) {
        this.cpf = cpf;
        this.password = password;
    }

    // Getters and Setters
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    @Override
    public String toString() {
        return "LoginRequest{" +
                "cpf='" + cpf + '\'' +
                ", password='[PROTECTED]'" +
                '}';
    }
}