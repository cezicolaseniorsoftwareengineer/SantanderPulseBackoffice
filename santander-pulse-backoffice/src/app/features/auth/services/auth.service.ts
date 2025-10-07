import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface LoginRequest {
  cpf: string;
  password: string;
}

export interface RegisterRequest {
  cpf: string;
  email: string;
  password: string;
  fullName: string;
}

export interface AuthUser {
  id: number;
  username: string;
  email: string;
  fullName: string;
  role: string;
  cpf?: string | null;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: AuthUser;
}

export interface AuthProviderInfo {
  enabled: boolean;
  authorizationUrl?: string;
  redirectUri?: string;
  scopes?: string[];
  [key: string]: unknown;
}

export interface AuthProvidersResponse {
  providers: {
    google?: AuthProviderInfo;
    [key: string]: AuthProviderInfo | undefined;
  };
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly TOKEN_KEY = 'auth_access_token';
  private readonly REFRESH_TOKEN_KEY = 'auth_refresh_token';
  private readonly USER_KEY = 'auth_user';
  private readonly EXPIRES_AT_KEY = 'auth_expires_at';
  private readonly POST_LOGIN_REDIRECT_KEY = 'auth_post_login_redirect';

  private authStateSubject = new BehaviorSubject<boolean>(this.hasValidStoredToken());
  authState$ = this.authStateSubject.asObservable();

  private currentUserSubject = new BehaviorSubject<AuthUser | null>(this.getStoredUser());
  user$ = this.currentUserSubject.asObservable();

  private googleEnabledSubject = new BehaviorSubject<boolean>(false);
  googleEnabled$ = this.googleEnabledSubject.asObservable();
  private googleAuthUrl: string | null = null;

  constructor(private http: HttpClient) {
    this.loadAuthProviders();
  }

  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/login`, credentials).pipe(
      tap(response => this.persistSession(response))
    );
  }

  register(payload: RegisterRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/register`, payload).pipe(
      tap(response => this.persistSession(response))
    );
  }

  loginWithGoogle(): void {
    this.initiateGoogleLogin();
  }

  logout(): void {
    this.clearSession();
  }

  isAuthenticated(): boolean {
    const authenticated = this.hasValidStoredToken();
    this.authStateSubject.next(authenticated);
    return authenticated;
  }

  getToken(): string | null {
    const valid = this.hasValidStoredToken();
    return valid ? localStorage.getItem(this.TOKEN_KEY) : null;
  }

  getRefreshToken(): string | null {
    return localStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  getCurrentUser(): AuthUser | null {
    return this.currentUserSubject.value;
  }

  setSessionFromOAuth(accessToken: string, refreshToken: string, expiresIn: number, user: AuthUser): void {
    this.storeSession(accessToken, refreshToken, expiresIn, user);
  }

  initiateGoogleLogin(returnUrl?: string | null): void {
    const normalizedReturnUrl = returnUrl ?? null;
    this.storePostLoginRedirect(normalizedReturnUrl);

    const targetUrl = this.googleAuthUrl?.trim().length ? this.googleAuthUrl : environment.oauthGoogleUrl;
    const redirectUri = environment.oauthRedirectUri;

    if (!targetUrl) {
      console.warn('No Google OAuth authorization URL is available.');
      return;
    }

    try {
      const url = new URL(targetUrl);

      if (redirectUri?.trim().length) {
        url.searchParams.set('redirect_uri', redirectUri.trim());
      }

      window.location.href = url.toString();
    } catch (error) {
      console.error('Failed to normalize Google OAuth URL. Falling back to naive redirect.', error);
      if (redirectUri?.trim().length) {
        const separator = targetUrl.includes('?') ? '&' : '?';
        window.location.href = `${targetUrl}${separator}redirect_uri=${encodeURIComponent(redirectUri.trim())}`;
      } else {
        window.location.href = targetUrl;
      }
    }
  }

  consumePostLoginRedirect(): string | null {
    const stored = sessionStorage.getItem(this.POST_LOGIN_REDIRECT_KEY);
    if (stored) {
      sessionStorage.removeItem(this.POST_LOGIN_REDIRECT_KEY);
      return stored;
    }
    return null;
  }

  private persistSession(response: AuthResponse): void {
    this.storeSession(response.accessToken, response.refreshToken, response.expiresIn, response.user);
  }

  private storeSession(accessToken: string, refreshToken: string, expiresIn: number, user: AuthUser): void {
    const expiresAt = Date.now() + expiresIn;
    localStorage.setItem(this.TOKEN_KEY, accessToken);
    localStorage.setItem(this.REFRESH_TOKEN_KEY, refreshToken);
    localStorage.setItem(this.USER_KEY, JSON.stringify(user));
    localStorage.setItem(this.EXPIRES_AT_KEY, expiresAt.toString());

    this.authStateSubject.next(true);
    this.currentUserSubject.next(user);
    this.storePostLoginRedirect(null);
  }

  private clearSession(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    localStorage.removeItem(this.REFRESH_TOKEN_KEY);
    localStorage.removeItem(this.USER_KEY);
    localStorage.removeItem(this.EXPIRES_AT_KEY);

    this.authStateSubject.next(false);
    this.currentUserSubject.next(null);
  }

  private storePostLoginRedirect(returnUrl: string | null): void {
    if (returnUrl && returnUrl.trim().length > 0 && returnUrl !== '/login') {
      sessionStorage.setItem(this.POST_LOGIN_REDIRECT_KEY, returnUrl);
    } else {
      sessionStorage.removeItem(this.POST_LOGIN_REDIRECT_KEY);
    }
  }

  private hasValidStoredToken(): boolean {
    const token = localStorage.getItem(this.TOKEN_KEY);
    const expiresAt = this.getExpiresAt();

    if (!token || !expiresAt) {
      return false;
    }

    if (Date.now() > expiresAt) {
      this.clearSession();
      return false;
    }

    return true;
  }

  private getStoredUser(): AuthUser | null {
    const raw = localStorage.getItem(this.USER_KEY);
    if (!raw) {
      return null;
    }

    try {
      return JSON.parse(raw) as AuthUser;
    } catch (error) {
      console.error('Failed to parse stored user', error);
      return null;
    }
  }

  private getExpiresAt(): number | null {
    const value = localStorage.getItem(this.EXPIRES_AT_KEY);
    if (!value) {
      return null;
    }

    const parsed = Number(value);
    return Number.isNaN(parsed) ? null : parsed;
  }

  private loadAuthProviders(): void {
    console.log('[AuthService] Carregando providers de autenticação...');
    console.log('[AuthService] URL:', `${environment.apiUrl}/auth/providers`);
    
    this.http.get<AuthProvidersResponse>(`${environment.apiUrl}/auth/providers`).subscribe({
      next: response => {
        console.log('[AuthService] Resposta recebida:', response);
        
        const googleProvider = response.providers?.google;
        const enabled = !!googleProvider?.enabled;
        
        console.log('[AuthService] Google Provider:', googleProvider);
        console.log('[AuthService] Google Enabled:', enabled);
        
        this.googleEnabledSubject.next(enabled);

        const authorizationUrl = typeof googleProvider?.authorizationUrl === 'string'
          ? googleProvider.authorizationUrl
          : null;

        this.googleAuthUrl = authorizationUrl && authorizationUrl.trim().length > 0
          ? authorizationUrl
          : null;

        if (!this.googleAuthUrl && enabled) {
          this.googleAuthUrl = environment.oauthGoogleUrl;
        }
        
        console.log('[AuthService] Google Auth URL:', this.googleAuthUrl);
        console.log('[AuthService] Configuração concluída!');
      },
      error: err => {
        console.error('[AuthService] Erro ao carregar providers:', err);
        this.googleEnabledSubject.next(false);
        this.googleAuthUrl = null;
      }
    });
  }
}
