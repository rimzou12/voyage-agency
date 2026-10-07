import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { API_BASE_URL } from './api-config';
import { decodeJwtExpiryMillis } from './jwt';
import { AuthResponse, User } from './models';

const STORAGE_KEY = 'agency-voyage:auth';

interface StoredAuth {
  token: string;
  user: User;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly stored = signal<StoredAuth | null>(readStoredAuth());
  private readonly expiredSignal = signal(false);
  private expiryTimer: ReturnType<typeof setTimeout> | null = null;

  readonly currentUser = computed(() => this.stored()?.user ?? null);
  readonly isLoggedIn = computed(() => this.stored() !== null);
  /** True once the session has ended because the token expired (vs. a manual logout). */
  readonly expired = this.expiredSignal.asReadonly();

  constructor() {
    const existing = this.stored();
    if (existing) {
      this.scheduleExpiry(existing.token);
    }
  }

  get token(): string | null {
    return this.stored()?.token ?? null;
  }

  register(email: string, password: string, displayName: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${API_BASE_URL}/api/auth/register`, { email, password, displayName })
      .pipe(tap((response) => this.storeAuth(response)));
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${API_BASE_URL}/api/auth/login`, { email, password })
      .pipe(tap((response) => this.storeAuth(response)));
  }

  logout(): void {
    this.clearExpiryTimer();
    this.stored.set(null);
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      // ignore - localStorage unavailable
    }
  }

  /** Logs out and flags the session as expired, for the UI to notice and tell the user. */
  markExpired(): void {
    this.logout();
    this.expiredSignal.set(true);
  }

  /** Lets the UI reset the flag once it's told the user, so it doesn't fire again. */
  acknowledgeExpiry(): void {
    this.expiredSignal.set(false);
  }

  private storeAuth(response: AuthResponse): void {
    const auth: StoredAuth = { token: response.token, user: response.user };
    this.expiredSignal.set(false);
    this.stored.set(auth);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
    } catch {
      // ignore - localStorage unavailable
    }
    this.scheduleExpiry(auth.token);
  }

  private scheduleExpiry(token: string): void {
    this.clearExpiryTimer();
    const expiresAt = decodeJwtExpiryMillis(token);
    if (expiresAt === null) {
      return;
    }
    const delay = expiresAt - Date.now();
    if (delay <= 0) {
      this.markExpired();
      return;
    }
    this.expiryTimer = setTimeout(() => this.markExpired(), delay);
  }

  private clearExpiryTimer(): void {
    if (this.expiryTimer !== null) {
      clearTimeout(this.expiryTimer);
      this.expiryTimer = null;
    }
  }
}

function readStoredAuth(): StoredAuth | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? (JSON.parse(raw) as StoredAuth) : null;
  } catch {
    return null;
  }
}
