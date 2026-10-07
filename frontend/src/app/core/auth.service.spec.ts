import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { vi } from 'vitest';
import { AuthService } from './auth.service';
import { API_BASE_URL } from './api-config';
import { AuthResponse } from './models';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.removeItem('agency-voyage:auth');
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.removeItem('agency-voyage:auth');
  });

  it('starts logged out when there is nothing in storage', () => {
    expect(service.isLoggedIn()).toBeFalsy();
    expect(service.currentUser()).toBeNull();
  });

  it('logging in stores the token and user, and marks the service as logged in', () => {
    service.login('alice@example.com', 'password123').subscribe();

    httpMock.expectOne(`${API_BASE_URL}/api/auth/login`).flush(sampleAuthResponse());

    expect(service.isLoggedIn()).toBeTruthy();
    expect(service.currentUser()?.displayName).toBe('Alice');
    expect(service.token).toBe('jwt-token');
    expect(localStorage.getItem('agency-voyage:auth')).toContain('jwt-token');
  });

  it('registering stores the token and user just like logging in', () => {
    service.register('alice@example.com', 'password123', 'Alice').subscribe();

    httpMock.expectOne(`${API_BASE_URL}/api/auth/register`).flush(sampleAuthResponse());

    expect(service.isLoggedIn()).toBeTruthy();
  });

  it('logout clears the stored session', () => {
    service.login('alice@example.com', 'password123').subscribe();
    httpMock.expectOne(`${API_BASE_URL}/api/auth/login`).flush(sampleAuthResponse());
    expect(service.isLoggedIn()).toBeTruthy();

    service.logout();

    expect(service.isLoggedIn()).toBeFalsy();
    expect(service.token).toBeNull();
    expect(localStorage.getItem('agency-voyage:auth')).toBeNull();
  });

  it('logout does not flag the session as expired', () => {
    service.login('alice@example.com', 'password123').subscribe();
    httpMock.expectOne(`${API_BASE_URL}/api/auth/login`).flush(sampleAuthResponse());

    service.logout();

    expect(service.expired()).toBeFalsy();
  });

  describe('token expiry', () => {
    afterEach(() => vi.useRealTimers());

    it('automatically logs out and flags the session as expired once the token expires', () => {
      vi.useFakeTimers();
      service.login('alice@example.com', 'password123').subscribe();
      httpMock.expectOne(`${API_BASE_URL}/api/auth/login`).flush(sampleAuthResponse(fakeJwt(5)));
      expect(service.isLoggedIn()).toBeTruthy();

      vi.advanceTimersByTime(5_000);

      expect(service.isLoggedIn()).toBeFalsy();
      expect(service.expired()).toBeTruthy();
    });

    it('does not expire before the token actually does', () => {
      vi.useFakeTimers();
      service.login('alice@example.com', 'password123').subscribe();
      httpMock.expectOne(`${API_BASE_URL}/api/auth/login`).flush(sampleAuthResponse(fakeJwt(5)));

      vi.advanceTimersByTime(4_000);

      expect(service.isLoggedIn()).toBeTruthy();
      expect(service.expired()).toBeFalsy();
    });

    it('logs out immediately on startup if the stored token is already expired', () => {
      localStorage.setItem(
        'agency-voyage:auth',
        JSON.stringify({
          token: fakeJwt(-60),
          user: { id: 'u1', email: 'alice@example.com', displayName: 'Alice', isAdmin: false },
        }),
      );

      // service/beforeEach already constructed AuthService against the then-empty
      // storage - start a fresh TestBed so the constructor re-reads localStorage.
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [provideHttpClient(), provideHttpClientTesting()],
      });
      const fresh = TestBed.inject(AuthService);

      expect(fresh.isLoggedIn()).toBeFalsy();
      expect(fresh.expired()).toBeTruthy();
    });

    it('acknowledgeExpiry resets the flag so the UI only reacts once', () => {
      vi.useFakeTimers();
      service.login('alice@example.com', 'password123').subscribe();
      httpMock.expectOne(`${API_BASE_URL}/api/auth/login`).flush(sampleAuthResponse(fakeJwt(5)));
      vi.advanceTimersByTime(5_000);
      expect(service.expired()).toBeTruthy();

      service.acknowledgeExpiry();

      expect(service.expired()).toBeFalsy();
    });
  });

  function sampleAuthResponse(token = 'jwt-token'): AuthResponse {
    return {
      token,
      user: { id: 'u1', email: 'alice@example.com', displayName: 'Alice', isAdmin: false },
    };
  }

  /** A JWT-shaped (but unsigned) token with the given expiry, seconds from now. */
  function fakeJwt(expiresInSeconds: number): string {
    const header = btoa(JSON.stringify({ alg: 'none' }));
    const payload = btoa(JSON.stringify({ exp: Math.floor(Date.now() / 1000) + expiresInSeconds }));
    return `${header}.${payload}.sig`;
  }
});
