import { HttpInterceptorFn } from '@angular/common/http';

export const securityInterceptor: HttpInterceptorFn = (req, next) => {
  // Evitar dependência circular - ler token diretamente do localStorage
  const token = localStorage.getItem('auth_access_token');

  const isAuthEndpoint = req.url.includes('/auth/');
  const isOAuthEndpoint = req.url.includes('/oauth2/');

  console.log('SecurityInterceptor:', {
    url: req.url,
    method: req.method,
    hasToken: !!token,
    tokenLength: token ? token.length : 0,
    isAuthEndpoint,
    isOAuthEndpoint,
    willAddAuth: token && !isAuthEndpoint && !isOAuthEndpoint
  });

  if (token && !isAuthEndpoint && !isOAuthEndpoint) {
    req = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    console.log('SecurityInterceptor: Authorization header added');
  } else {
    console.log('SecurityInterceptor: No authorization header added');
  }

  return next(req);
};
