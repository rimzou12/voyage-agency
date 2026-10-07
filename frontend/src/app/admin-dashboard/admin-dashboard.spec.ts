import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { vi } from 'vitest';
import { AdminDashboard } from './admin-dashboard';
import { API_BASE_URL } from '../core/api-config';
import { Trip } from '../core/models';

describe('AdminDashboard', () => {
  let httpMock: HttpTestingController;

  afterEach(() => localStorage.removeItem('agency-voyage:auth'));

  async function setUp(isAdmin: boolean): Promise<void> {
    localStorage.setItem(
      'agency-voyage:auth',
      JSON.stringify({
        token: 'fake-token',
        user: { id: 'u1', email: 'admin@example.com', displayName: 'Admin', isAdmin },
      }),
    );
    await TestBed.configureTestingModule({
      imports: [AdminDashboard],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), provideNoopAnimations()],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  }

  it('redirects a non-admin away from the dashboard', async () => {
    await setUp(false);
    const router = TestBed.inject(Router);
    const navigateSpy = vi.spyOn(router, 'navigateByUrl');

    const fixture = TestBed.createComponent(AdminDashboard);
    fixture.detectChanges();

    expect(navigateSpy).toHaveBeenCalledWith('/');
    httpMock.expectNone(`${API_BASE_URL}/api/trips`);
  });

  it('loads and displays trips for an admin', async () => {
    await setUp(true);
    const fixture = TestBed.createComponent(AdminDashboard);
    fixture.detectChanges();

    httpMock.expectOne(`${API_BASE_URL}/api/trips`).flush([sampleTrip()]);
    fixture.detectChanges();

    const component = fixture.componentInstance as unknown as { trips: () => Trip[]; loading: () => boolean };
    expect(component.loading()).toBeFalsy();
    expect(component.trips().length).toBe(1);
    expect(component.trips()[0].destination).toBe('Bali');
  });

  it('submits a new trip and refreshes the list', async () => {
    await setUp(true);
    const fixture = TestBed.createComponent(AdminDashboard);
    fixture.detectChanges();
    httpMock.expectOne(`${API_BASE_URL}/api/trips`).flush([]);
    fixture.detectChanges();

    const component = fixture.componentInstance as unknown as {
      startCreatingTrip: () => void;
      tripForm: { set: (v: unknown) => void };
      submitTripForm: () => void;
    };
    component.startCreatingTrip();
    component.tripForm.set({
      destination: 'Bali',
      description: 'desc',
      departureDate: '2027-06-10',
      returnDate: '2027-06-20',
      minParticipants: 2,
      maxParticipants: 5,
      bookingDeadline: '2027-05-01T10:00',
      basePrice: 1000,
      priceTiers: '',
      photoUrls: '',
    });
    component.submitTripForm();

    const createReq = httpMock.expectOne(`${API_BASE_URL}/api/trips`);
    expect(createReq.request.method).toBe('POST');
    expect(createReq.request.body.destination).toBe('Bali');
    createReq.flush(sampleTrip());

    httpMock.expectOne(`${API_BASE_URL}/api/trips`).flush([sampleTrip()]);
  });

  it('deletes a trip after confirming, then refreshes the list', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    await setUp(true);
    const fixture = TestBed.createComponent(AdminDashboard);
    fixture.detectChanges();
    const trip = sampleTrip();
    httpMock.expectOne(`${API_BASE_URL}/api/trips`).flush([trip]);
    fixture.detectChanges();

    const component = fixture.componentInstance as unknown as { deleteTrip: (t: Trip) => void };
    component.deleteTrip(trip);

    const deleteReq = httpMock.expectOne(`${API_BASE_URL}/api/trips/${trip.id}`);
    expect(deleteReq.request.method).toBe('DELETE');
    deleteReq.flush(null);

    httpMock.expectOne(`${API_BASE_URL}/api/trips`).flush([]);
  });

  it('does not delete a trip when the confirmation is declined', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false);
    await setUp(true);
    const fixture = TestBed.createComponent(AdminDashboard);
    fixture.detectChanges();
    const trip = sampleTrip();
    httpMock.expectOne(`${API_BASE_URL}/api/trips`).flush([trip]);
    fixture.detectChanges();

    const component = fixture.componentInstance as unknown as { deleteTrip: (t: Trip) => void };
    component.deleteTrip(trip);

    httpMock.expectNone(`${API_BASE_URL}/api/trips/${trip.id}`);
  });

  function sampleTrip(): Trip {
    return {
      id: 'trip-1',
      destination: 'Bali',
      description: 'desc',
      departureDate: '2027-06-10',
      returnDate: '2027-06-20',
      minParticipants: 2,
      maxParticipants: 10,
      bookingDeadline: '2027-05-01T00:00:00Z',
      basePrice: 1000,
      priceTiers: [{ minParticipants: 5, pricePerSeat: 800 }],
      photoUrls: [],
    };
  }
});
