package com.santander.pulse.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO for user registration request
 */
public class RegisterRequest {

    @NotBlank(message = "CPF is required")
    @Pattern(regexp = "\\d{11}", message = "CPF must contain exactly 11 digits")
    private String cpf;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;

    // Constructors
    public RegisterRequest() {}

    public RegisterRequest(String cpf, String email, String password, String fullName) {
        this.cpf = cpf;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
    }

    // Getters and Setters
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    @Override
    public String toString() {
        return "RegisterRequest{" +
                "cpf='" + cpf + '\'' +
                ", email='" + email + '\'' +
                ", password='[PROTECTED]'" +
                ", fullName='" + fullName + '\'' +
                '}';
    }
}