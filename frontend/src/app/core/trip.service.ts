import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-config';
import { PriceTier, Trip } from './models';

export interface TripInput {
  destination: string;
  description: string;
  departureDate: string;
  returnDate: string;
  minParticipants: number;
  maxParticipants: number;
  bookingDeadline: string;
  basePrice: number;
  priceTiers: PriceTier[];
  photoUrls: string[];
}

@Injectable({ providedIn: 'root' })
export class TripService {
  private readonly http = inject(HttpClient);

  listTrips(): Observable<Trip[]> {
    return this.http.get<Trip[]>(`${API_BASE_URL}/api/trips`);
  }

  getTrip(tripId: string): Observable<Trip> {
    return this.http.get<Trip>(`${API_BASE_URL}/api/trips/${tripId}`);
  }

  /** Requires the caller to be an admin - the auth interceptor attaches the token. */
  createTrip(trip: TripInput): Observable<Trip> {
    return this.http.post<Trip>(`${API_BASE_URL}/api/trips`, trip);
  }

  /** Requires the caller to be an admin - the auth interceptor attaches the token. */
  updateTrip(tripId: string, trip: TripInput): Observable<Trip> {
    return this.http.put<Trip>(`${API_BASE_URL}/api/trips/${tripId}`, trip);
  }

  /** Requires the caller to be an admin - the auth interceptor attaches the token. */
  deleteTrip(tripId: string): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/api/trips/${tripId}`);
  }
}
