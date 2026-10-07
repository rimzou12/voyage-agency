import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TripService } from '../core/trip.service';
import { GroupBookingService, apiErrorMessage } from '../core/group-booking.service';
import { HotelService } from '../core/hotel.service';
import { HotelReviewService } from '../core/hotel-review.service';
import { AuthService } from '../core/auth.service';
import { I18nService } from '../core/i18n.service';
import { Hotel, HotelReview, Trip } from '../core/models';
import { tripPhotoUrls } from '../core/photos';
import { ImageCarousel } from '../shared/image-carousel/image-carousel';

interface ReviewFormState {
  rating: number;
  comment: string;
}

const BLANK_REVIEW_FORM: ReviewFormState = { rating: 5, comment: '' };

@Component({
  selector: 'app-trip-detail',
  standalone: true,
  imports: [
    RouterLink,
    CurrencyPipe,
    DatePipe,
    FormsModule,
    ImageCarousel,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './trip-detail.html',
  styleUrl: './trip-detail.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TripDetail {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly tripService = inject(TripService);
  private readonly groupBookingService = inject(GroupBookingService);
  private readonly hotelService = inject(HotelService);
  private readonly hotelReviewService = inject(HotelReviewService);
  protected readonly auth = inject(AuthService);
  protected readonly i18n = inject(I18nService);

  private readonly tripId = this.route.snapshot.paramMap.get('id')!;

  protected readonly trip = signal<Trip | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly creating = signal(false);
  protected readonly createError = signal<string | null>(null);
  protected readonly photos = computed(() => {
    const trip = this.trip();
    if (!trip) {
      return [];
    }
    return trip.photoUrls.length > 0 ? trip.photoUrls : tripPhotoUrls(trip.id, 6);
  });

  protected readonly hotels = signal<Hotel[]>([]);
  protected readonly reviewsByHotelId = signal<Record<string, HotelReview[]>>({});

  protected readonly reviewingHotelId = signal<string | null>(null);
  protected readonly reviewForm = signal<ReviewFormState>({ ...BLANK_REVIEW_FORM });
  protected readonly savingReview = signal(false);
  protected readonly reviewError = signal<string | null>(null);
  protected readonly deletingReviewId = signal<string | null>(null);

  protected readonly stars = [1, 2, 3, 4, 5];

  constructor() {
    this.tripService.getTrip(this.tripId).subscribe({
      next: (trip) => {
        this.trip.set(trip);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(this.i18n.t('tripDetail.notFound'));
        this.loading.set(false);
      },
    });
    this.refreshHotels();
  }

  private refreshHotels(): void {
    this.hotelService.listHotels(this.tripId).subscribe({
      next: (hotels) => {
        this.hotels.set(hotels);
        hotels.forEach((hotel) => this.refreshReviews(hotel.id));
      },
      error: () => {
        // Non-critical: the hotel catalog is supplementary, so a failed fetch just
        // leaves the section empty rather than blocking the rest of the page.
      },
    });
  }

  private refreshReviews(hotelId: string): void {
    this.hotelReviewService.listReviews(hotelId).subscribe({
      next: (reviews) => this.reviewsByHotelId.update((byId) => ({ ...byId, [hotelId]: reviews })),
      error: () => {
        // Non-critical, same reasoning as the hotel catalog fetch above.
      },
    });
  }

  protected reviewsFor(hotelId: string): HotelReview[] {
    return this.reviewsByHotelId()[hotelId] ?? [];
  }

  protected averageRating(hotelId: string): number | null {
    const reviews = this.reviewsFor(hotelId);
    if (reviews.length === 0) {
      return null;
    }
    const sum = reviews.reduce((total, review) => total + review.rating, 0);
    return Math.round((sum / reviews.length) * 10) / 10;
  }

  protected myReview(hotelId: string): HotelReview | undefined {
    const myId = this.auth.currentUser()?.id;
    return myId ? this.reviewsFor(hotelId).find((review) => review.authorId === myId) : undefined;
  }

  protected canDeleteReview(review: HotelReview): boolean {
    const user = this.auth.currentUser();
    return user !== null && (user.id === review.authorId || user.isAdmin);
  }

  protected startReview(hotelId: string): void {
    const existing = this.myReview(hotelId);
    this.reviewForm.set(existing ? { rating: existing.rating, comment: existing.comment ?? '' } : { ...BLANK_REVIEW_FORM });
    this.reviewError.set(null);
    this.reviewingHotelId.set(hotelId);
  }

  protected cancelReview(): void {
    this.reviewingHotelId.set(null);
    this.reviewForm.set({ ...BLANK_REVIEW_FORM });
  }

  protected setRating(rating: number): void {
    this.reviewForm.set({ ...this.reviewForm(), rating });
  }

  protected submitReview(hotelId: string): void {
    const form = this.reviewForm();
    const comment = form.comment.trim() || null;
    const existing = this.myReview(hotelId);

    this.savingReview.set(true);
    this.reviewError.set(null);
    const request = existing
      ? this.hotelReviewService.updateReview(hotelId, existing.id, form.rating, comment)
      : this.hotelReviewService.addReview(hotelId, form.rating, comment);
    request.subscribe({
      next: () => {
        this.savingReview.set(false);
        this.reviewingHotelId.set(null);
        this.reviewForm.set({ ...BLANK_REVIEW_FORM });
        this.refreshReviews(hotelId);
      },
      error: (err: HttpErrorResponse) => {
        this.savingReview.set(false);
        this.reviewError.set(apiErrorMessage(err, this.i18n.t('tripDetail.reviewError')));
      },
    });
  }

  protected deleteReview(hotelId: string, review: HotelReview): void {
    this.deletingReviewId.set(review.id);
    this.hotelReviewService.deleteReview(hotelId, review.id).subscribe({
      next: () => {
        this.deletingReviewId.set(null);
        this.refreshReviews(hotelId);
      },
      error: () => this.deletingReviewId.set(null),
    });
  }

  protected startGroup(): void {
    const trip = this.trip();
    if (!trip) {
      return;
    }
    this.creating.set(true);
    this.createError.set(null);
    this.groupBookingService.createGroupBooking(trip.id).subscribe({
      next: (booking) => this.router.navigate(['/group-bookings', booking.id]),
      error: (err: HttpErrorResponse) => {
        this.creating.set(false);
        this.createError.set(apiErrorMessage(err, this.i18n.t('tripDetail.startError')));
      },
    });
  }
}
