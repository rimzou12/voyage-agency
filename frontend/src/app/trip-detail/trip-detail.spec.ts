import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, ActivatedRoute, convertToParamMap } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { TripDetail } from './trip-detail';
import { API_BASE_URL } from '../core/api-config';
import { Hotel, HotelReview, Trip } from '../core/models';

describe('TripDetail', () => {
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TripDetail],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideNoopAnimations(),
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: 'trip-1' }) } },
        },
      ],
    }).compileComponents();
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.removeItem('agency-voyage:auth');
  });

  it('loads and displays the trip', () => {
    const fixture = TestBed.createComponent(TripDetail);
    fixture.detectChanges();

    const req = httpMock.expectOne(`${API_BASE_URL}/api/trips/trip-1`);
    req.flush(sampleTrip());
    httpMock.expectOne(`${API_BASE_URL}/api/trips/trip-1/hotels`).flush([]);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Bali');
  });

  it('shows the average rating and review count for a hotel', () => {
    const fixture = TestBed.createComponent(TripDetail);
    fixture.detectChanges();

    httpMock.expectOne(`${API_BASE_URL}/api/trips/trip-1`).flush(sampleTrip());
    httpMock.expectOne(`${API_BASE_URL}/api/trips/trip-1/hotels`).flush([sampleHotel()]);
    fixture.detectChanges();

    httpMock
      .expectOne(`${API_BASE_URL}/api/hotels/hotel-1/reviews`)
      .flush([sampleReview('r1', 5), sampleReview('r2', 4)]);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('4.5');
    expect(compiled.textContent).toContain('2 reviews');
  });

  it('shows "No reviews yet" when a hotel has none', () => {
    const fixture = TestBed.createComponent(TripDetail);
    fixture.detectChanges();

    httpMock.expectOne(`${API_BASE_URL}/api/trips/trip-1`).flush(sampleTrip());
    httpMock.expectOne(`${API_BASE_URL}/api/trips/trip-1/hotels`).flush([sampleHotel()]);
    fixture.detectChanges();
    httpMock.expectOne(`${API_BASE_URL}/api/hotels/hotel-1/reviews`).flush([]);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('No reviews yet.');
  });

  it('lets a logged-in user submit a review for a hotel', () => {
    localStorage.setItem(
      'agency-voyage:auth',
      JSON.stringify({
        token: 'fake-token',
        user: { id: 'u1', email: 'alice@example.com', displayName: 'Alice', isAdmin: false },
      }),
    );

    const fixture = TestBed.createComponent(TripDetail);
    fixture.detectChanges();

    httpMock.expectOne(`${API_BASE_URL}/api/trips/trip-1`).flush(sampleTrip());
    httpMock.expectOne(`${API_BASE_URL}/api/trips/trip-1/hotels`).flush([sampleHotel()]);
    fixture.detectChanges();
    httpMock.expectOne(`${API_BASE_URL}/api/hotels/hotel-1/reviews`).flush([]);
    fixture.detectChanges();

    const compiled = fixture.nativeElement as HTMLElement;
    (compiled.querySelector('.write-review') as HTMLButtonElement).click();
    fixture.detectChanges();

    const component = fixture.componentInstance as unknown as {
      setRating: (rating: number) => void;
      submitReview: (hotelId: string) => void;
    };
    component.setRating(5);
    (fixture.componentInstance as unknown as { reviewForm: { set: (v: { rating: number; comment: string }) => void } })
      .reviewForm.set({ rating: 5, comment: 'Loved it!' });
    component.submitReview('hotel-1');

    const addReq = httpMock.expectOne(`${API_BASE_URL}/api/hotels/hotel-1/reviews`);
    expect(addReq.request.method).toBe('POST');
    expect(addReq.request.body).toEqual({ rating: 5, comment: 'Loved it!' });
    addReq.flush(sampleReview('r1', 5));

    httpMock.expectOne(`${API_BASE_URL}/api/hotels/hotel-1/reviews`).flush([sampleReview('r1', 5)]);
    fixture.detectChanges();
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
    };
  }

  function sampleHotel(): Hotel {
    return {
      id: 'hotel-1',
      tripId: 'trip-1',
      name: 'Ubud Retreat',
      description: 'Jungle views',
      photoUrls: [],
      amenities: [],
    };
  }

  function sampleReview(id: string, rating: number): HotelReview {
    return {
      id,
      hotelId: 'hotel-1',
      authorId: 'someone-else',
      authorName: 'Someone',
      rating,
      comment: null,
      reviewedAt: '2027-01-01T00:00:00Z',
    };
  }
});
