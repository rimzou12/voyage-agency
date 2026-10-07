import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';
import { API_BASE_URL } from './api-config';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let auth: AuthService;

  beforeEach(() => {
    localStorage.setItem(
      'agency-voyage:auth',
      JSON.stringify({
        token: 'some-token',
        user: { id: 'u1', email: 'alice@example.com', displayName: 'Alice', isAdmin: false },
      }),
    );
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.removeItem('agency-voyage:auth');
  });

  it('logs out and flags the session as expired when our API returns 401', () => {
    expect(auth.isLoggedIn()).toBeTruthy();

    http.get(`${API_BASE_URL}/api/trips`).subscribe({ error: () => undefined });
    httpMock.expectOne(`${API_BASE_URL}/api/trips`).flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });

    expect(auth.isLoggedIn()).toBeFalsy();
    expect(auth.expired()).toBeTruthy();
  });

  it('does not log out on a non-401 error', () => {
    http.get(`${API_BASE_URL}/api/trips`).subscribe({ error: () => undefined });
    httpMock.expectOne(`${API_BASE_URL}/api/trips`).flush('error', { status: 500, statusText: 'Server Error' });

    expect(auth.isLoggedIn()).toBeTruthy();
    expect(auth.expired()).toBeFalsy();
  });

  it('does not log out on a 401 from a different origin', () => {
    http.get('https://unrelated.example/data').subscribe({ error: () => undefined });
    httpMock.expectOne('https://unrelated.example/data').flush('nope', { status: 401, statusText: 'Unauthorized' });

    expect(auth.isLoggedIn()).toBeTruthy();
    expect(auth.expired()).toBeFalsy();
  });
});
