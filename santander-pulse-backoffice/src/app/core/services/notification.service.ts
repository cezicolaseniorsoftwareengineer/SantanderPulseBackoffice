import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  success(message: string): void {
    console.log('[SUCCESS]', message);
    alert(message);
  }

  error(message: string): void {
    console.error('[ERROR]', message);
    alert('ERRO: ' + message);
  }

  info(message: string): void {
    console.info('[INFO]', message);
    alert(message);
  }
}
