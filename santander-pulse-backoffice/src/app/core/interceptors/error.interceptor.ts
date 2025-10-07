import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { NotificationService } from '../services/notification.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const notificationService = inject(NotificationService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.error?.fields) {
        const fields = error.error.fields;
        Object.keys(fields).forEach(field => {
          notificationService.error(`${field}: ${fields[field]}`);
        });
      } else if (error.error?.message) {
        notificationService.error(error.error.message);
      } else if (error.status === 0) {
        notificationService.error('Erro de conexao com o servidor');
      } else if (error.status === 401) {
        notificationService.error('Nao autorizado');
      } else if (error.status === 403) {
        notificationService.error('Acesso negado');
      } else if (error.status === 404) {
        notificationService.error('Recurso nao encontrado');
      } else if (error.status >= 500) {
        notificationService.error('Erro interno do servidor');
      } else {
        notificationService.error('Erro ao processar requisicao');
      }

      return throwError(() => error);
    })
  );
};
