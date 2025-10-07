import { Component, OnInit } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService, RegisterRequest } from '../services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="login-container">
      <div class="login-card">
        <div class="card-header">
          <h2>{{ mode === 'login' ? 'Acesse sua conta' : 'Crie sua senha' }}</h2>
          <p>
            {{ mode === 'login'
              ? 'Utilize seu CPF e senha para acessar o Santander Pulse Backoffice.'
              : 'Informe seus dados para gerar uma senha segura de acesso.' }}
          </p>
        </div>

        <div class="tabs">
          <button type="button" [class.active]="mode === 'login'" (click)="setMode('login')">
            Entrar com CPF
          </button>
          <button type="button" [class.active]="mode === 'register'" (click)="setMode('register')">
            Cadastrar senha
          </button>
        </div>

        <form *ngIf="mode === 'login'" [formGroup]="loginForm" (ngSubmit)="onLogin()" class="form">
          <div class="form-group">
            <label for="cpf">CPF</label>
            <input
              id="cpf"
              type="text"
              formControlName="cpf"
              class="form-control"
              placeholder="Digite apenas os números"
              maxlength="11"
            />
            <div class="error" *ngIf="submittedLogin && loginForm.get('cpf')?.invalid">
              Informe um CPF válido com 11 dígitos.
            </div>
          </div>

          <div class="form-group">
            <label for="password">Senha</label>
            <input
              id="password"
              type="password"
              formControlName="password"
              class="form-control"
              placeholder="Digite sua senha"
            />
            <div class="error" *ngIf="submittedLogin && loginForm.get('password')?.invalid">
              A senha é obrigatória.
            </div>
          </div>

          <button type="submit" class="btn-primary" [disabled]="loginForm.invalid || loadingLogin">
            {{ loadingLogin ? 'Entrando...' : 'Entrar' }}
          </button>
          <div class="google-auth">
            <button
              *ngIf="(googleEnabled$ | async); else googleUnavailable"
              type="button"
              class="btn-google"
              (click)="onGoogleLogin()"
            >
              <img src="assets/images/google.svg" alt="Google" />
              Entrar com Google
            </button>
            <ng-template #googleUnavailable>
              <button type="button" class="btn-google disabled" disabled>
                Autenticação Google indisponível
              </button>
              <p class="google-helper">
                Solicite ao time técnico a configuração do Google OAuth para habilitar esta opção.
              </p>
            </ng-template>
          </div>
        </form>

        <form *ngIf="mode === 'register'" [formGroup]="registerForm" (ngSubmit)="onRegister()" class="form">
          <div class="form-group">
            <label for="fullName">Nome completo</label>
            <input
              id="fullName"
              type="text"
              formControlName="fullName"
              class="form-control"
              placeholder="Informe seu nome"
            />
            <div class="error" *ngIf="submittedRegister && registerForm.get('fullName')?.invalid">
              Informe o nome completo.
            </div>
          </div>

          <div class="form-group">
            <label for="registerCpf">CPF</label>
            <input
              id="registerCpf"
              type="text"
              formControlName="cpf"
              class="form-control"
              placeholder="Digite apenas os números"
              maxlength="11"
            />
            <div class="error" *ngIf="submittedRegister && registerForm.get('cpf')?.invalid">
              Informe um CPF válido com 11 dígitos.
            </div>
          </div>

          <div class="form-group">
            <label for="email">E-mail corporativo</label>
            <input
              id="email"
              type="email"
              formControlName="email"
              class="form-control"
              placeholder="nome@santander.com"
            />
            <div class="error" *ngIf="submittedRegister && registerForm.get('email')?.invalid">
              Informe um e-mail válido.
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label for="registerPassword">Senha</label>
              <input
                id="registerPassword"
                type="password"
                formControlName="password"
                class="form-control"
                placeholder="Mínimo 8 caracteres"
              />
              <div class="error" *ngIf="submittedRegister && registerForm.get('password')?.invalid">
                A senha deve ter pelo menos 8 caracteres.
              </div>
            </div>

            <div class="form-group">
              <label for="confirmPassword">Confirmar senha</label>
              <input
                id="confirmPassword"
                type="password"
                formControlName="confirmPassword"
                class="form-control"
                placeholder="Repita a senha"
              />
              <div class="error" *ngIf="submittedRegister && registerForm.get('confirmPassword')?.invalid">
                As senhas precisam ser iguais.
              </div>
            </div>
          </div>

          <button type="submit" class="btn-primary" [disabled]="registerForm.invalid || loadingRegister">
            {{ loadingRegister ? 'Criando senha...' : 'Cadastrar' }}
          </button>

          <div class="helper-text">
            <strong>Dica de segurança:</strong> use letras maiúsculas, minúsculas, números e um caractere especial.
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .login-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: calc(100vh - 200px);
      padding: 2rem;
      background: linear-gradient(135deg, #ec0000 0%, #9c0000 100%);
    }
    .login-card {
      background: white;
      border-radius: 16px;
      padding: 2.5rem;
      box-shadow: 0 20px 60px rgba(0,0,0,0.2);
      width: 100%;
      max-width: 480px;
    }
    .card-header {
      text-align: center;
      margin-bottom: 2rem;
    }
    .card-header h2 {
      margin: 0;
      color: #202124;
      font-size: 1.8rem;
      font-weight: 600;
    }
    .card-header p {
      margin-top: 0.75rem;
      color: #5f6368;
      font-size: 0.95rem;
    }
    .tabs {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      background: #f1f3f4;
      border-radius: 12px;
      padding: 0.25rem;
      margin-bottom: 2rem;
      gap: 0.5rem;
    }
    .tabs button {
      border: none;
      border-radius: 10px;
      padding: 0.75rem 1rem;
      font-weight: 600;
      color: #5f6368;
      background: transparent;
      cursor: pointer;
      transition: all 0.2s ease;
    }
    .tabs button.active {
      background: white;
      color: #ec0000;
      box-shadow: 0 10px 30px rgba(236, 0, 0, 0.15);
    }
    .form {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }
    .form-group {
      display: flex;
      flex-direction: column;
      gap: 0.35rem;
    }
    label {
      color: #3c4043;
      font-weight: 600;
    }
    .form-control {
      width: 100%;
      padding: 0.75rem;
      border: 1px solid #dfe1e5;
      border-radius: 8px;
      font-size: 0.95rem;
      transition: border-color 0.2s ease, box-shadow 0.2s ease;
    }
    .form-control:focus {
      outline: none;
      border-color: #ec0000;
      box-shadow: 0 0 0 3px rgba(236, 0, 0, 0.15);
    }
    .error {
      color: #d32f2f;
      font-size: 0.8rem;
    }
    .btn-primary {
      width: 100%;
      padding: 0.75rem;
      background: #ec0000;
      color: white;
      border: none;
      border-radius: 8px;
      font-size: 1rem;
      font-weight: 600;
      cursor: pointer;
      margin-top: 1rem;
      transition: background 0.2s ease, transform 0.2s ease;
    }
    .btn-primary:hover:not(:disabled) {
      background: #b80000;
      transform: translateY(-1px);
    }
    .btn-primary:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
    .btn-google {
      width: 100%;
      margin-top: 1rem;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.75rem;
      padding: 0.75rem;
      border-radius: 8px;
      border: 1px solid #dfe1e5;
      background: white;
      color: #3c4043;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s ease;
    }
    .btn-google:hover {
      background: #f8f9fa;
      box-shadow: 0 10px 30px rgba(0,0,0,0.06);
    }
    .btn-google img {
      width: 18px;
      height: 18px;
    }
    .btn-google.disabled {
      cursor: not-allowed;
      color: #9aa0a6;
      border-color: #e0e0e0;
      background: #f5f5f5;
      box-shadow: none;
    }
    .google-auth {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      width: 100%;
    }
    .google-helper {
      font-size: 0.8rem;
      color: #9aa0a6;
      text-align: center;
      margin: 0;
    }
    .helper-text {
      font-size: 0.8rem;
      color: #5f6368;
      text-align: center;
      margin-top: 0.75rem;
    }
    .form-row {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 1rem;
    }
    @media (max-width: 540px) {
      .login-card {
        padding: 1.75rem;
      }
      .tabs {
        grid-template-columns: 1fr;
      }
      .form-row {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class LoginPage implements OnInit {
  loginForm: FormGroup;
  registerForm: FormGroup;
  loadingLogin = false;
  loadingRegister = false;
  submittedLogin = false;
  submittedRegister = false;
  mode: 'login' | 'register' = 'login';
  private returnUrl: string | null = null;
  googleEnabled$!: Observable<boolean>;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private notification: NotificationService
  ) {
    this.loginForm = this.fb.group({
      cpf: ['', [Validators.required, Validators.pattern(/^\d{11}$/)]],
      password: ['', Validators.required]
    });

    this.registerForm = this.fb.group({
      fullName: ['', [Validators.required, Validators.maxLength(100)]],
      cpf: ['', [Validators.required, Validators.pattern(/^\d{11}$/)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: this.matchPasswords('password', 'confirmPassword') });

    this.returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
    this.googleEnabled$ = this.authService.googleEnabled$;

  }

  ngOnInit(): void {
    if (this.authService.isAuthenticated()) {
      const target = this.returnUrl ?? '/customers';
      this.router.navigate([target]);
      return;
    }
  }

  setMode(mode: 'login' | 'register'): void {
    this.mode = mode;
    this.submittedLogin = false;
    this.submittedRegister = false;
  }

  onLogin(): void {
    this.submittedLogin = true;
    if (this.loginForm.invalid) {
      return;
    }

    this.loadingLogin = true;
    const rawCredentials = this.loginForm.value as { cpf: string; password: string };
    const credentials = {
      cpf: rawCredentials.cpf.replace(/\D/g, ''),
      password: rawCredentials.password
    };

    this.authService.login(credentials).subscribe({
      next: () => {
        this.notification.success('Login realizado com sucesso');
        const target = this.returnUrl ?? '/customers';
        this.router.navigate([target]);
      },
      error: () => {
        this.loadingLogin = false;
      },
      complete: () => {
        this.loadingLogin = false;
      }
    });
  }

  onRegister(): void {
    this.submittedRegister = true;
    if (this.registerForm.invalid) {
      return;
    }

    this.loadingRegister = true;
    const cpfValue = (this.registerForm.get('cpf')?.value ?? '').replace(/\D/g, '');
    const payload = {
      fullName: this.registerForm.get('fullName')?.value ?? '',
      cpf: cpfValue,
      email: this.registerForm.get('email')?.value ?? '',
      password: this.registerForm.get('password')?.value ?? ''
    } as RegisterRequest;

    this.authService.register(payload).subscribe({
      next: () => {
        this.notification.success('Senha criada com sucesso! Você já pode acessar a plataforma.');
        this.setMode('login');
        this.registerForm.reset();
        this.router.navigate(['/customers']);
      },
      error: () => {
        this.loadingRegister = false;
      },
      complete: () => {
        this.loadingRegister = false;
      }
    });
  }

  onGoogleLogin(): void {
    this.authService.initiateGoogleLogin(this.returnUrl ?? '/customers');
  }

  private matchPasswords(passwordKey: string, confirmPasswordKey: string) {
    return (group: AbstractControl) => {
      const password = group.get(passwordKey)?.value;
      const confirmPassword = group.get(confirmPasswordKey)?.value;

      if (password !== confirmPassword) {
        group.get(confirmPasswordKey)?.setErrors({ mismatch: true });
      } else {
        group.get(confirmPasswordKey)?.setErrors(null);
      }

      return null;
    };
  }
}
