import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { API_BASE_URL } from './api-config';

/**
 * Attaches the bearer token to every request to our own API, when logged in, and logs
 * out if the API ever rejects it with 401 - the backup for AuthService's own proactive
 * expiry timer (clock drift, a token invalidated server-side, the machine having been
 * asleep past the scheduled timeout, etc).
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token;
  const isOwnApi = req.url.startsWith(API_BASE_URL);

  const request = token && isOwnApi ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(request).pipe(
    catchError((error: unknown) => {
      if (isOwnApi && error instanceof HttpErrorResponse && error.status === 401 && auth.isLoggedIn()) {
        auth.markExpired();
      }
      return throwError(() => error);
    }),
  );
};
