import { Component } from '@angular/core';
import { Router, RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService, AuthUser } from './features/auth/services/auth.service';
import { NotificationService } from './core/services/notification.service';
import { combineLatest, Observable } from 'rxjs';
import { map } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule],
  template: `
    <div class="santander-app">
      <ng-container *ngIf="vm$ | async as vm">
  <!-- Header shell orchestrates secure navigation, echoing Kent Beck's emphasis on feedback-driven interfaces -->
  <header class="santander-header" *ngIf="vm.isAuth">
          <div class="header-container">
            <div class="header-left">
              <div class="logo-container">
                <svg class="santander-logo" viewBox="0 0 200 60" xmlns="http://www.w3.org/2000/svg">
                  <rect x="0" y="0" width="200" height="60" fill="#EC0000" rx="4"/>
                  <text x="100" y="40" font-family="Arial, sans-serif" font-size="28" font-weight="bold" fill="white" text-anchor="middle">Santander</text>
                </svg>
                <span class="app-title">Pulse Backoffice</span>
              </div>
            </div>
            
            <nav class="header-nav">
              <a routerLink="/customers" routerLinkActive="active" class="nav-link">
                <i class="icon">Clientes</i>
              </a>
              <a routerLink="/dashboard" routerLinkActive="active" class="nav-link">
                <i class="icon">Dashboard</i>
              </a>
              <a routerLink="/reports" routerLinkActive="active" class="nav-link">
                <i class="icon">Relatórios</i>
              </a>
            </nav>

            <div class="header-right" *ngIf="vm.user as user">
              <div class="user-info">
                <div class="user-avatar">{{ getInitials(user) }}</div>
                <span class="user-name">{{ user.fullName || user.email }}</span>
              </div>
              <button class="btn-logout" (click)="logout()">
                <i class="icon">Sair</i>
              </button>
            </div>
          </div>
        </header>

  <!-- Primary outlet channels feature modules, aligning with Martin Fowler's separation of presentation and domain flows -->
  <main class="santander-main" [class.public-mode]="!vm.isAuth">
          <div class="main-container">
            <router-outlet></router-outlet>
          </div>
        </main>

  <!-- Footer delivers compliance context, honoring Uncle Bob's call for communicative code -->
  <footer class="santander-footer" *ngIf="vm.isAuth">
          <div class="footer-container">
          <div class="footer-content">
            <div class="footer-section">
              <h4>Santander Pulse</h4>
              <ul>
                <li><a href="#">Sobre</a></li>
                <li><a href="#">Privacidade</a></li>
                <li><a href="#">Termos de Uso</a></li>
                <li><a href="#">Segurança</a></li>
              </ul>
            </div>
            
            <div class="footer-section">
              <h4>Suporte</h4>
              <ul>
                <li><a href="#">Central de Ajuda</a></li>
                <li><a href="#">Contato</a></li>
                <li><a href="#">FAQ</a></li>
                <li><a href="#">Documentação</a></li>
              </ul>
            </div>
            
            <div class="footer-section">
              <h4>Compliance</h4>
              <ul>
                <li><a href="#">LGPD</a></li>
                <li><a href="#">PCI DSS</a></li>
                <li><a href="#">ISO 27001</a></li>
                <li><a href="#">Bacen</a></li>
              </ul>
            </div>
          </div>

          <div class="footer-divider"></div>

          <div class="footer-bottom">
            <p class="copyright">
              Desenvolvido por <strong>Cezi Cola Senior Software Engineer</strong> | 
              <strong>Bio Code Technology ltda</strong> | 
              <strong>Santander Backoffice Pulse Bank</strong> | 
              Todos os Direitos Reservados © 2025
            </p>
            <div class="footer-badges">
              <span class="badge">SSL Secure</span>
              <span class="badge">PCI Compliant</span>
              <span class="badge">LGPD Protected</span>
            </div>
          </div>
        </div>
        </footer>
      </ng-container>
    </div>
  `,
  styles: [`
    @import '../assets/styles/variables';

    .santander-app {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      background: $gray-100;
    }

    /* ============================================
       HEADER SANTANDER
       ============================================ */
    .santander-header {
      background: $gradient-header;
      box-shadow: $shadow-lg;
      position: sticky;
      top: 0;
      z-index: $z-sticky;
    }

    .header-container {
      max-width: 1400px;
      margin: 0 auto;
      padding: 0 $spacing-lg;
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: 70px;
    }

    .header-left {
      display: flex;
      align-items: center;
      gap: $spacing-lg;
    }

    .logo-container {
      display: flex;
      align-items: center;
      gap: $spacing-md;
    }

    .santander-logo {
      height: 40px;
      width: auto;
      filter: drop-shadow(0 2px 4px rgba(0,0,0,0.2));
    }

    .app-title {
      color: $white;
      font-size: $font-size-xl;
      font-weight: $font-weight-semibold;
      letter-spacing: 0.5px;
    }

    .header-nav {
      display: flex;
      gap: $spacing-sm;
    }

    .nav-link {
      display: flex;
      align-items: center;
      gap: $spacing-sm;
      padding: $spacing-sm $spacing-md;
      color: rgba(255, 255, 255, 0.9);
      text-decoration: none;
      border-radius: $border-radius-md;
      font-weight: $font-weight-medium;
      transition: all $transition-fast;

      &:hover {
        background: rgba(255, 255, 255, 0.1);
        color: $white;
      }

      &.active {
        background: rgba(255, 255, 255, 0.2);
        color: $white;
      }
    }

    .header-right {
      display: flex;
      align-items: center;
      gap: $spacing-md;
    }

    .user-info {
      display: flex;
      align-items: center;
      gap: $spacing-sm;
      color: $white;
    }

    .user-avatar {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.2);
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: $font-weight-bold;
      font-size: $font-size-sm;
    }

    .user-name {
      font-weight: $font-weight-medium;
    }

    .btn-logout {
      display: flex;
      align-items: center;
      gap: $spacing-sm;
      padding: $spacing-sm $spacing-md;
      background: rgba(255, 255, 255, 0.1);
      border: 1px solid rgba(255, 255, 255, 0.3);
      border-radius: $border-radius-md;
      color: $white;
      font-weight: $font-weight-medium;
      transition: all $transition-fast;

      &:hover {
        background: rgba(255, 255, 255, 0.2);
        border-color: rgba(255, 255, 255, 0.5);
      }
    }

    /* ============================================
       MAIN CONTENT
       ============================================ */
    .santander-main {
      flex: 1;
      padding: $spacing-2xl 0;
    }

    .santander-main.public-mode {
      padding: 0;
    }

    .main-container {
      max-width: 1400px;
      margin: 0 auto;
      padding: 0 $spacing-lg;
    }

    .santander-main.public-mode .main-container {
      max-width: none;
      padding: 0;
    }

    /* ============================================
       FOOTER SANTANDER
       ============================================ */
    .santander-footer {
      background: $gray-900;
      color: $gray-300;
      border-top: 4px solid $santander-red;
    }

    .footer-container {
      max-width: 1400px;
      margin: 0 auto;
      padding: $spacing-2xl $spacing-lg;
    }

    .footer-content {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: $spacing-xl;
      margin-bottom: $spacing-xl;
    }

    .footer-section h4 {
      color: $white;
      font-size: $font-size-lg;
      margin-bottom: $spacing-md;
      font-weight: $font-weight-semibold;
    }

    .footer-section ul {
      list-style: none;
      padding: 0;
      margin: 0;
    }

    .footer-section li {
      margin-bottom: $spacing-sm;
    }

    .footer-section a {
      color: $gray-400;
      text-decoration: none;
      transition: color $transition-fast;
      font-size: $font-size-sm;

      &:hover {
        color: $santander-red-light;
      }
    }

    .footer-divider {
      height: 1px;
      background: $gray-700;
      margin: $spacing-xl 0;
    }

    .footer-bottom {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: $spacing-md;
      text-align: center;
    }

    .copyright {
      color: $gray-400;
      font-size: $font-size-sm;
      line-height: 1.6;
      margin: 0;

      strong {
        color: $white;
        font-weight: $font-weight-semibold;
      }
    }

    .footer-badges {
      display: flex;
      gap: $spacing-md;
      flex-wrap: wrap;
      justify-content: center;
    }

    .badge {
      padding: $spacing-xs $spacing-md;
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid $gray-700;
      border-radius: $border-radius-pill;
      font-size: $font-size-xs;
      color: $gray-400;
      display: flex;
      align-items: center;
      gap: $spacing-xs;
    }

    /* ============================================
       RESPONSIVE
       ============================================ */
    @media (max-width: 768px) {
      .header-nav {
        display: none;
      }

      .footer-content {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class AppComponent {
  title = 'Santander Pulse Backoffice';
  readonly vm$: Observable<{ isAuth: boolean; user: AuthUser | null }>;

  constructor(
    private authService: AuthService,
    private router: Router,
    private notification: NotificationService
  ) {
    this.vm$ = combineLatest([
      this.authService.authState$,
      this.authService.user$
    ]).pipe(
      map(([isAuth, user]) => ({ isAuth, user }))
    );
  }

  logout(): void {
    this.authService.logout();
    this.notification.info('Sessão encerrada com segurança');
    this.router.navigate(['/login']);
  }

  getInitials(user: AuthUser | null): string {
    if (!user) {
      return '--';
    }

    const source = user.fullName || user.email || user.username;
    if (!source) {
      return '--';
    }

    const segments = source
      .split(' ')
      .filter(Boolean)
      .slice(0, 2);

    if (segments.length === 1) {
      return segments[0].substring(0, 2).toUpperCase();
    }

    return segments
      .map(part => part.charAt(0))
      .join('')
      .substring(0, 2)
      .toUpperCase();
  }
}
