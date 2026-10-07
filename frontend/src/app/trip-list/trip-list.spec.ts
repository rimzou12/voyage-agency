import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideNativeDateAdapter } from '@angular/material/core';
import { TripList } from './trip-list';
import { API_BASE_URL } from '../core/api-config';
import { Trip } from '../core/models';

describe('TripList', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TripList],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideNoopAnimations(),
        provideNativeDateAdapter(),
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('loads and displays trips', () => {
    const fixture = TestBed.createComponent(TripList);
    fixture.detectChanges();

    const req = httpMock.expectOne(`${API_BASE_URL}/api/trips`);
    req.flush([sampleTrip()]);
    fixture.detectChanges();

    const component = fixture.componentInstance as unknown as { trips: () => Trip[]; loading: () => boolean };
    expect(component.loading()).toBeFalsy();
    expect(component.trips().length).toBe(1);
    expect(component.trips()[0].destination).toBe('Bali');
  });

  it('filters trips by name as the search term changes', () => {
    const fixture = TestBed.createComponent(TripList);
    fixture.detectChanges();

    httpMock
      .expectOne(`${API_BASE_URL}/api/trips`)
      .flush([sampleTrip(), { ...sampleTrip(), id: 'trip-2', destination: 'Kyoto' }]);
    fixture.detectChanges();

    const component = fixture.componentInstance as unknown as {
      searchTerm: { set: (v: string) => void };
      filteredTrips: () => Trip[];
    };
    component.searchTerm.set('kyo');

    expect(component.filteredTrips().map((t) => t.destination)).toEqual(['Kyoto']);
  });

  it('filters trips to ones running on the chosen availability date', () => {
    const fixture = TestBed.createComponent(TripList);
    fixture.detectChanges();

    httpMock.expectOne(`${API_BASE_URL}/api/trips`).flush([sampleTrip()]);
    fixture.detectChanges();

    const component = fixture.componentInstance as unknown as {
      availabilityDate: { set: (v: Date | null) => void };
      filteredTrips: () => Trip[];
    };

    component.availabilityDate.set(new Date(2027, 5, 15));
    expect(component.filteredTrips().length).toBe(1);

    component.availabilityDate.set(new Date(2027, 6, 1));
    expect(component.filteredTrips().length).toBe(0);
  });

  it('shows an error message when the request fails', () => {
    const fixture = TestBed.createComponent(TripList);
    fixture.detectChanges();

    const req = httpMock.expectOne(`${API_BASE_URL}/api/trips`);
    req.flush('boom', { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Could not load trips');
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
