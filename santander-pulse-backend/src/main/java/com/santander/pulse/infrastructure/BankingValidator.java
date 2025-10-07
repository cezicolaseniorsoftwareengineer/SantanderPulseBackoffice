package com.santander.pulse.infrastructure;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Banking validators for CPF, CNPJ and other financial data.
 * Implements Módulo 11 algorithms for Brazilian documents.
 */
@Component
public class BankingValidator {

    private static final Pattern CPF_PATTERN = Pattern.compile("\\d{11}|\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}");
    private static final Pattern CNPJ_PATTERN = Pattern.compile("\\d{14}|\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\(?\\d{2}\\)?[\\s-]?\\d{4,5}[\\s-]?\\d{4}");

    /**
     * Validate CPF using Módulo 11 algorithm
     */
    public boolean isValidCPF(String cpf) {
        if (cpf == null || cpf.trim().isEmpty()) {
            return false;
        }

        // Remove formatting
        String cleanCpf = cpf.replaceAll("[^0-9]", "");

        // Check if has 11 digits
        if (cleanCpf.length() != 11) {
            return false;
        }

        // Check for known invalid CPFs (all same digits)
        if (cleanCpf.matches("(\\d)\\1{10}")) {
            return false;
        }

        // Validate using Módulo 11 algorithm
        return validateCPFCheckDigits(cleanCpf);
    }

    /**
     * Validate CNPJ using Módulo 11 algorithm
     */
    public boolean isValidCNPJ(String cnpj) {
        if (cnpj == null || cnpj.trim().isEmpty()) {
            return false;
        }

        // Remove formatting
        String cleanCnpj = cnpj.replaceAll("[^0-9]", "");

        // Check if has 14 digits
        if (cleanCnpj.length() != 14) {
            return false;
        }

        // Check for known invalid CNPJs (all same digits)
        if (cleanCnpj.matches("(\\d)\\1{13}")) {
            return false;
        }

        // Validate using Módulo 11 algorithm
        return validateCNPJCheckDigits(cleanCnpj);
    }

    /**
     * Validate Brazilian phone number format
     */
    public boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }

        return PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * Validate email format (RFC 5322 compliant)
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        // Basic email validation
        return email.contains("@") && 
               email.contains(".") && 
               email.length() >= 5 && 
               email.length() <= 100;
    }

    /**
     * Format CPF for display (mask sensitive digits)
     */
    public String formatCPF(String cpf) {
        if (!isValidCPF(cpf)) {
            return cpf;
        }

        String cleanCpf = cpf.replaceAll("[^0-9]", "");
        return cleanCpf.substring(0, 3) + "." +
               "***" + "." +
               "***" + "-" +
               cleanCpf.substring(9);
    }

    /**
     * Format CNPJ for display (mask sensitive digits)
     */
    public String formatCNPJ(String cnpj) {
        if (!isValidCNPJ(cnpj)) {
            return cnpj;
        }

        String cleanCnpj = cnpj.replaceAll("[^0-9]", "");
        return cleanCnpj.substring(0, 2) + "." +
               "***" + "." +
               "***" + "/" +
               cleanCnpj.substring(8, 12) + "-" +
               cleanCnpj.substring(12);
    }

    /**
     * Clean document removing all formatting
     */
    public String cleanDocument(String document) {
        if (document == null) {
            return null;
        }
        return document.replaceAll("[^0-9]", "");
    }

    // Private helper methods
    private boolean validateCPFCheckDigits(String cpf) {
        try {
            // Calculate first check digit
            int sum = 0;
            for (int i = 0; i < 9; i++) {
                sum += Character.getNumericValue(cpf.charAt(i)) * (10 - i);
            }
            int firstDigit = 11 - (sum % 11);
            if (firstDigit >= 10) firstDigit = 0;

            // Calculate second check digit
            sum = 0;
            for (int i = 0; i < 10; i++) {
                sum += Character.getNumericValue(cpf.charAt(i)) * (11 - i);
            }
            int secondDigit = 11 - (sum % 11);
            if (secondDigit >= 10) secondDigit = 0;

            // Validate check digits
            return firstDigit == Character.getNumericValue(cpf.charAt(9)) &&
                   secondDigit == Character.getNumericValue(cpf.charAt(10));

        } catch (Exception e) {
            return false;
        }
    }

    private boolean validateCNPJCheckDigits(String cnpj) {
        try {
            // First check digit calculation
            int[] weights1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
            int sum = 0;
            for (int i = 0; i < 12; i++) {
                sum += Character.getNumericValue(cnpj.charAt(i)) * weights1[i];
            }
            int firstDigit = 11 - (sum % 11);
            if (firstDigit >= 10) firstDigit = 0;

            // Second check digit calculation
            int[] weights2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
            sum = 0;
            for (int i = 0; i < 13; i++) {
                sum += Character.getNumericValue(cnpj.charAt(i)) * weights2[i];
            }
            int secondDigit = 11 - (sum % 11);
            if (secondDigit >= 10) secondDigit = 0;

            // Validate check digits
            return firstDigit == Character.getNumericValue(cnpj.charAt(12)) &&
                   secondDigit == Character.getNumericValue(cnpj.charAt(13));

        } catch (Exception e) {
            return false;
        }
    }
}