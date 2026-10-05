import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { API_BASE_URL } from './api-config';
import { HotelReview } from './models';

@Injectable({ providedIn: 'root' })
export class HotelReviewService {
  private readonly http = inject(HttpClient);

  /** Newest first. Public - no authentication required. */
  listReviews(hotelId: string): Observable<HotelReview[]> {
    return this.http.get<HotelReview[]>(`${API_BASE_URL}/api/hotels/${hotelId}/reviews`);
  }

  /** Requires the caller to be logged in - the auth interceptor attaches the token. At most one per hotel. */
  addReview(hotelId: string, rating: number, comment: string | null): Observable<HotelReview> {
    return this.http.post<HotelReview>(`${API_BASE_URL}/api/hotels/${hotelId}/reviews`, { rating, comment });
  }

  /** Requires the caller to be the review's author. */
  updateReview(hotelId: string, reviewId: string, rating: number, comment: string | null): Observable<HotelReview> {
    return this.http.put<HotelReview>(`${API_BASE_URL}/api/hotels/${hotelId}/reviews/${reviewId}`, {
      rating,
      comment,
    });
  }

  /** Requires the caller to be the review's author, or an admin. */
  deleteReview(hotelId: string, reviewId: string): Observable<void> {
    return this.http.delete<void>(`${API_BASE_URL}/api/hotels/${hotelId}/reviews/${reviewId}`);
  }
}
