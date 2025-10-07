import { Injectable } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { LoggerService } from './logger.service';
import { NotificationService } from './notification.service';

export enum ErrorCategory {
  VALIDATION = 'VALIDATION',
  NETWORK = 'NETWORK',
  BUSINESS = 'BUSINESS',
  AUTHENTICATION = 'AUTHENTICATION',
  AUTHORIZATION = 'AUTHORIZATION',
  UNKNOWN = 'UNKNOWN'
}

export interface DiagnosticError {
  category: ErrorCategory;
  component: string;
  action: string;
  userMessage: string;
  technicalMessage: string;
  statusCode?: number;
  details?: any;
  timestamp: string;
}

/**
 * Centralized error boundary inspired by Fowler's enterprise patterns and Beck's feedback loops:
 * - Classifies failures to expose business intent rapidly.
 * - Emits structured telemetry to sustain auditability.
 * - Surfaces distinct messages for users and engineers without duplicating noise.
 * - Preserves traceability to honor Clean Architecture contracts.
 */
@Injectable({
  providedIn: 'root'
})
export class ErrorHandlerService {
  
  constructor(
    private logger: LoggerService,
    private notification: NotificationService
  ) {}

  /**
   * Maps HTTP faults into domain-aware diagnostics, keeping Uncle Bob's separation between policy and detail.
   */
  handleHttpError(error: HttpErrorResponse, component: string, action: string): DiagnosticError {
    const diagnostic: DiagnosticError = {
      category: this.categorizeHttpError(error),
      component,
      action,
      userMessage: '',
      technicalMessage: '',
      statusCode: error.status,
      details: error.error,
      timestamp: new Date().toISOString()
    };

    switch (error.status) {
  case 400: // Bad Request branch keeps validation invariants explicit for fast remediation
        diagnostic.userMessage = 'Dados inválidos. Verifique os campos do formulário.';
        diagnostic.technicalMessage = `Validação falhou: ${JSON.stringify(error.error?.fields || error.error)}`;
        this.logger.warn(component, action, diagnostic.technicalMessage, error.error);
        break;

  case 401: // Unauthorized branch safeguards session renewal semantics
        diagnostic.userMessage = 'Sessão expirada. Faça login novamente.';
        diagnostic.technicalMessage = 'Token JWT inválido ou expirado';
        this.logger.error(component, action, diagnostic.technicalMessage, error);
        break;

  case 403: // Forbidden branch documents unmet authorization policies
        diagnostic.userMessage = 'Você não tem permissão para esta ação.';
        diagnostic.technicalMessage = `Acesso negado para ${action}`;
        this.logger.error(component, action, diagnostic.technicalMessage, error);
        break;

  case 404: // Not Found branch provides deterministic feedback when aggregates vanish
        diagnostic.userMessage = 'Registro não encontrado.';
        diagnostic.technicalMessage = `Recurso não encontrado: ${error.url}`;
        this.logger.warn(component, action, diagnostic.technicalMessage);
        break;

  case 422: // Business rule violation surfaces domain reasoning without leaking internals
        diagnostic.userMessage = error.error?.message || 'Não foi possível processar a operação.';
        diagnostic.technicalMessage = `Regra de negócio violada: ${error.error?.message}`;
        this.logger.warn(component, action, diagnostic.technicalMessage, error.error);
        break;

  case 500: // Server error branch signals platform instability for rapid incident response
        diagnostic.userMessage = 'Erro no servidor. Tente novamente mais tarde.';
        diagnostic.technicalMessage = 'Erro interno no backend';
        this.logger.critical(component, action, diagnostic.technicalMessage, error);
        break;

  case 0: // Network outage branch highlights connectivity issues before retry orchestration
        diagnostic.userMessage = 'Sem conexão com o servidor. Verifique sua internet.';
        diagnostic.technicalMessage = `Falha de rede ao acessar ${error.url}`;
        this.logger.error(component, action, diagnostic.technicalMessage, error);
        break;

  default:
        diagnostic.userMessage = 'Erro inesperado. Tente novamente.';
        diagnostic.technicalMessage = `HTTP ${error.status}: ${error.message}`;
        this.logger.error(component, action, diagnostic.technicalMessage, error);
    }

  // Notify user via event-driven channel to keep UI concerns declarative
    this.notification.error(diagnostic.userMessage);

    return diagnostic;
  }

  /**
   * Normalizes form-validation faults so presenters stay lean and expressive.
   */
  handleValidationError(formErrors: any, component: string, action: string): DiagnosticError {
    const diagnostic: DiagnosticError = {
      category: ErrorCategory.VALIDATION,
      component,
      action,
      userMessage: 'Corrija os campos destacados antes de continuar.',
      technicalMessage: `Validação de formulário falhou: ${JSON.stringify(formErrors)}`,
      details: formErrors,
      timestamp: new Date().toISOString()
    };

    this.logger.info(component, action, diagnostic.technicalMessage, formErrors);
    
    return diagnostic;
  }

  /**
   * Captures unexpected runtime exceptions without leaking stack details into the UI boundary.
   */
  handleApplicationError(error: Error, component: string, action: string): DiagnosticError {
    const diagnostic: DiagnosticError = {
      category: ErrorCategory.UNKNOWN,
      component,
      action,
      userMessage: 'Erro na aplicação. Recarregue a página.',
      technicalMessage: `${error.name}: ${error.message}`,
      details: { stack: error.stack },
      timestamp: new Date().toISOString()
    };

    this.logger.critical(component, action, diagnostic.technicalMessage, error);
    this.notification.error(diagnostic.userMessage);

    return diagnostic;
  }

  /**
   * Categorizes HTTP status codes, mirroring Hexagonal ports to decouple transport from policy.
   */
  private categorizeHttpError(error: HttpErrorResponse): ErrorCategory {
    if (error.status === 400) return ErrorCategory.VALIDATION;
    if (error.status === 401) return ErrorCategory.AUTHENTICATION;
    if (error.status === 403) return ErrorCategory.AUTHORIZATION;
    if (error.status === 422) return ErrorCategory.BUSINESS;
    if (error.status === 0) return ErrorCategory.NETWORK;
    return ErrorCategory.UNKNOWN;
  }

  /**
   * Builds a diagnostic report to assist incident response teams without reinventing log parsing.
   */
  generateDiagnosticReport(component?: string): string {
    const logs = this.logger.getDiagnostics(component);
    const errors = logs.filter(log => log.level >= 2); // WARN, ERROR, CRITICAL

    return `
=== RELATÓRIO DE DIAGNÓSTICO ===
Gerado em: ${new Date().toISOString()}
Componente: ${component || 'TODOS'}

Total de logs: ${logs.length}
Total de erros: ${errors.length}

=== ERROS RECENTES ===
${errors.slice(-10).map(log => `
[${log.timestamp}] ${log.component}::${log.action}
Mensagem: ${log.message}
Dados: ${JSON.stringify(log.data, null, 2)}
${log.error ? 'Erro: ' + JSON.stringify(log.error, null, 2) : ''}
---
`).join('\n')}

=== LOGS COMPLETOS ===
${this.logger.exportLogs()}
    `.trim();
  }
}
