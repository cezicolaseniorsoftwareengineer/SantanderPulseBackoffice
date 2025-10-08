package com.santander.pulse.application.dto;

/**
 * Response DTO para operações de exclusão de clientes
 * Define contrato claro entre backend e frontend
 */
public record CustomerDeletionResponse(
    Long customerId,
    String customerName,
    String action, // "DEACTIVATED" ou "DELETED"
    String message,
    boolean shouldRemoveFromList, // Indica se frontend deve remover da lista
    long timestamp
) {
    
    public static CustomerDeletionResponse softDelete(Long customerId, String customerName) {
        return new CustomerDeletionResponse(
            customerId,
            customerName,
            "DEACTIVATED",
            "Customer deactivated successfully",
            true, // Frontend deve remover da lista padrão
            System.currentTimeMillis()
        );
    }
    
    public static CustomerDeletionResponse hardDelete(Long customerId, String customerName) {
        return new CustomerDeletionResponse(
            customerId,
            customerName,
            "DELETED",
            "Customer permanently deleted",
            true, // Frontend deve remover da lista
            System.currentTimeMillis()
        );
    }
}