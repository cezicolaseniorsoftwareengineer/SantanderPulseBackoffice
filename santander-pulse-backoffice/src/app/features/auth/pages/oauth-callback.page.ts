import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService, AuthUser } from '../services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-oauth-callback',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="callback-container">
      <div class="callback-card">
        <div class="spinner"></div>
        <h2>Finalizando autenticação segura...</h2>
        <p>Estamos validando suas credenciais com o Google e redirecionando para o painel.</p>
      </div>
    </div>
  `,
  styles: [`
    .callback-container {
      min-height: calc(100vh - 200px);
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #ec0000 0%, #9c0000 100%);
      padding: 2rem;
    }
    .callback-card {
      background: #ffffff;
      border-radius: 16px;
      padding: 2.5rem;
      max-width: 420px;
      text-align: center;
      box-shadow: 0 20px 60px rgba(0,0,0,0.2);
      color: #3c4043;
    }
    .spinner {
      width: 48px;
      height: 48px;
      border-radius: 50%;
      margin: 0 auto 1.5rem auto;
      border: 4px solid rgba(236,0,0,0.2);
      border-top-color: #ec0000;
      animation: spin 0.9s linear infinite;
    }
    h2 {
      margin-bottom: 0.75rem;
      font-weight: 600;
    }
    p {
      font-size: 0.95rem;
      color: #5f6368;
    }
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
  `]
})
export class OAuthCallbackPage implements OnInit {
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService,
    private notification: NotificationService
  ) {}

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;

    const accessToken = params.get('accessToken');
    const refreshToken = params.get('refreshToken');
    const expiresInRaw = params.get('expiresIn');
    const userPayload = params.get('user');

    if (!accessToken || !refreshToken || !expiresInRaw || !userPayload) {
      this.notification.error('Não foi possível concluir o login com Google. Tente novamente.');
      this.router.navigate(['/login']);
      return;
    }

    const expiresIn = Number(expiresInRaw);
    if (Number.isNaN(expiresIn)) {
      this.notification.error('Resposta inválida do provedor de autenticação.');
      this.router.navigate(['/login']);
      return;
    }

    try {
      const user = JSON.parse(this.decodeBase64Url(userPayload)) as AuthUser;
      this.authService.setSessionFromOAuth(accessToken, refreshToken, expiresIn, user);
      this.notification.success('Login via Google realizado com sucesso');
      const target = this.authService.consumePostLoginRedirect() ?? '/customers';
      this.router.navigate([target]);
    } catch (error) {
      console.error('Failed to process OAuth callback', error);
      this.notification.error('Erro ao processar credenciais do Google.');
      this.router.navigate(['/login']);
    }
  }

  private decodeBase64Url(value: string): string {
    const normalized = value.replace(/-/g, '+').replace(/_/g, '/');
    const padded = normalized + '='.repeat((4 - (normalized.length % 4)) % 4);
    return atob(padded);
  }
}
