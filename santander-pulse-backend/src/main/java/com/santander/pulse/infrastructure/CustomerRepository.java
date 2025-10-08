package com.santander.pulse.infrastructure;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.santander.pulse.domain.Customer;

/**
 * Repository interface for Customer entity operations.
 * Implements banking-specific query methods and validations.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * Find customer by CPF
     */
    Optional<Customer> findByCpf(String cpf);

    /**
     * Find customer by email
     */
    Optional<Customer> findByEmail(String email);

    /**
     * Check if CPF exists
     */
    boolean existsByCpf(String cpf);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find customers by status
     */
    List<Customer> findByStatus(Customer.CustomerStatus status);

    /**
     * Find customers by status with pagination
     */
    Page<Customer> findByStatus(Customer.CustomerStatus status, Pageable pageable);

    /**
     * Find customers by name containing (case insensitive)
     */
    @Query("SELECT c FROM Customer c WHERE LOWER(c.nome) LIKE LOWER(CONCAT('%', :nome, '%'))")
    List<Customer> findByNomeContainingIgnoreCase(@Param("nome") String nome);

    /**
     * Find active customers
     */
    @Query("SELECT c FROM Customer c WHERE c.status = 'ATIVO'")
    List<Customer> findActiveCustomers();

    /**
     * Find customers created between dates
     */
    @Query("SELECT c FROM Customer c WHERE c.createdAt BETWEEN :startDate AND :endDate")
    List<Customer> findCustomersCreatedBetween(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * Count customers by status
     */
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.status = :status")
    long countByStatus(@Param("status") Customer.CustomerStatus status);

    /**
     * Find customers by multiple criteria
     */
    @Query("SELECT c FROM Customer c WHERE " +
           "(:nome IS NULL OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :nome, '%'))) AND " +
           "(:email IS NULL OR LOWER(c.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
           "(:status IS NULL OR c.status = :status)")
    Page<Customer> findByCriteria(
        @Param("nome") String nome,
        @Param("email") String email,
        @Param("status") Customer.CustomerStatus status,
        Pageable pageable
    );

    /**
     * Find active customers by criteria (default dashboard view)
     * Following Clean Code principles: method name expresses intent clearly
     */
    @Query("SELECT c FROM Customer c WHERE " +
           "c.status = 'ATIVO' AND " +
           "(:nome IS NULL OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :nome, '%'))) AND " +
           "(:email IS NULL OR LOWER(c.email) LIKE LOWER(CONCAT('%', :email, '%')))")
    Page<Customer> findActiveCustomersByCriteria(
        @Param("nome") String nome,
        @Param("email") String email,
        Pageable pageable
    );

    /**
     * Find recent customers (last 30 days)
     */
    @Query("SELECT c FROM Customer c WHERE c.createdAt >= :thirtyDaysAgo ORDER BY c.createdAt DESC")
    List<Customer> findRecentCustomers(@Param("thirtyDaysAgo") LocalDateTime thirtyDaysAgo);
}