import { Injectable } from '@angular/core';

export enum LogLevel {
  DEBUG = 0,
  INFO = 1,
  WARN = 2,
  ERROR = 3,
  CRITICAL = 4
}

export interface LogEntry {
  timestamp: string;
  level: LogLevel;
  component: string;
  action: string;
  message: string;
  data?: any;
  error?: any;
  correlationId?: string;
}

/**
 * Structured logging service shaped by Fowler's observability playbook:
 * - Emits JSON events enriched with correlation identifiers for downstream pipelines.
 * - Honors severity taxonomy (DEBUG to CRITICAL) so responders can triage noise quickly.
 * - Tracks component/action context to uphold Clean Architecture boundaries.
 */
@Injectable({
  providedIn: 'root'
})
export class LoggerService {
  private currentLevel: LogLevel = LogLevel.DEBUG;
  private logs: LogEntry[] = [];

  constructor() {
  // TODO(kent-beck-inspired): wire an asynchronous appender so production traffic streams into the telemetry spine.
  // Until then we keep entries in memory to unlock rapid feedback in local sessions.
  }

  private log(level: LogLevel, component: string, action: string, message: string, data?: any, error?: any): void {
    if (level < this.currentLevel) {
      return;
    }

    const entry: LogEntry = {
      timestamp: new Date().toISOString(),
      level,
      component,
      action,
      message,
      data,
      error: error ? this.serializeError(error) : undefined,
      correlationId: this.generateCorrelationId()
    };

    this.logs.push(entry);

  // Console output aligned with severity, giving developers immediate visual cues.
    const levelName = LogLevel[level];
    const prefix = `[${entry.timestamp}] [${levelName}] [${component}::${action}]`;
    
    switch (level) {
      case LogLevel.DEBUG:
        console.debug(prefix, message, data);
        break;
      case LogLevel.INFO:
        console.info(prefix, message, data);
        break;
      case LogLevel.WARN:
        console.warn(prefix, message, data);
        break;
      case LogLevel.ERROR:
      case LogLevel.CRITICAL:
        console.error(prefix, message, data, error);
        break;
    }

  // Cap the in-memory buffer to avoid starving long-lived sessions.
    if (this.logs.length > 1000) {
      this.logs.shift();
    }
  }

  debug(component: string, action: string, message: string, data?: any): void {
    this.log(LogLevel.DEBUG, component, action, message, data);
  }

  info(component: string, action: string, message: string, data?: any): void {
    this.log(LogLevel.INFO, component, action, message, data);
  }

  warn(component: string, action: string, message: string, data?: any): void {
    this.log(LogLevel.WARN, component, action, message, data);
  }

  error(component: string, action: string, message: string, error?: any, data?: any): void {
    this.log(LogLevel.ERROR, component, action, message, data, error);
  }

  critical(component: string, action: string, message: string, error?: any, data?: any): void {
    this.log(LogLevel.CRITICAL, component, action, message, data, error);
  }

  // Diagnostics: filters logs by component/action/level for forensic analysis.
  getDiagnostics(component?: string, action?: string, level?: LogLevel): LogEntry[] {
    return this.logs.filter(log => {
      if (component && log.component !== component) return false;
      if (action && log.action !== action) return false;
      if (level !== undefined && log.level !== level) return false;
      return true;
    });
  }

  // Exports logs as formatted JSON payload to aid post-mortem reviews.
  exportLogs(): string {
    return JSON.stringify(this.logs, null, 2);
  }

  // Clears the log buffer so new experiments start with a quiet baseline.
  clear(): void {
    this.logs = [];
  }

  private generateCorrelationId(): string {
    return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }

  private serializeError(error: any): any {
    if (error instanceof Error) {
      return {
        name: error.name,
        message: error.message,
        stack: error.stack
      };
    }
    return error;
  }
}
