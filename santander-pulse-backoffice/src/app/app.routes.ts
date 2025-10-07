import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/auth/pages/login.page').then(m => m.LoginPage)
  },
  {
    path: 'oauth2/callback',
    loadComponent: () => import('./features/auth/pages/oauth-callback.page').then(m => m.OAuthCallbackPage)
  },
  {
    path: 'customers',
    canActivate: [authGuard],
    loadComponent: () => import('./features/customers/pages/customer-list.page').then(m => m.CustomerListPage)
  },
  {
    path: '',
    redirectTo: '/customers',
    pathMatch: 'full'
  },
  {
    path: '**',
    redirectTo: '/customers'
  }
];
