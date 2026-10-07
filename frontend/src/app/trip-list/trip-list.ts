import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TripService } from '../core/trip.service';
import { I18nService } from '../core/i18n.service';
import { Trip } from '../core/models';
import { heroPhotoUrls, tripPhotoUrls } from '../core/photos';
import { ImageCarousel } from '../shared/image-carousel/image-carousel';

@Component({
  selector: 'app-trip-list',
  standalone: true,
  imports: [
    RouterLink,
    CurrencyPipe,
    DatePipe,
    FormsModule,
    ImageCarousel,
    MatButtonModule,
    MatDatepickerModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './trip-list.html',
  styleUrl: './trip-list.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TripList {
  private readonly tripService = inject(TripService);
  protected readonly i18n = inject(I18nService);
  protected readonly heroPhotos = heroPhotoUrls(5);

  protected readonly trips = signal<Trip[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly searchTerm = signal('');
  /** The date a trip must be running on (between its departure and return) to match. */
  protected readonly availabilityDate = signal<Date | null>(null);

  protected readonly filteredTrips = computed(() => {
    const term = this.searchTerm().trim().toLowerCase();
    const date = this.availabilityDate();
    return this.trips().filter((trip) => {
      const matchesName =
        !term || trip.destination.toLowerCase().includes(term) || trip.description.toLowerCase().includes(term);
      const matchesDate = !date || this.isAvailableOn(trip, date);
      return matchesName && matchesDate;
    });
  });

  private isAvailableOn(trip: Trip, date: Date): boolean {
    const target = this.atMidnight(date).getTime();
    return target >= this.atMidnight(new Date(trip.departureDate)).getTime()
      && target <= this.atMidnight(new Date(trip.returnDate)).getTime();
  }

  private atMidnight(date: Date): Date {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate());
  }

  constructor() {
    this.tripService.listTrips().subscribe({
      next: (trips) => {
        this.trips.set(trips);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load trips. Is the backend running on localhost:8080?');
        this.loading.set(false);
      },
    });
  }

  protected clearFilters(): void {
    this.searchTerm.set('');
    this.availabilityDate.set(null);
  }

  protected scrollToResults(): void {
    document.getElementById('results')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  protected startingPrice(trip: Trip): number {
    const cheapestTier = trip.priceTiers.at(-1);
    return cheapestTier ? cheapestTier.pricePerSeat : trip.basePrice;
  }

  private readonly photosByTripId = new Map<string, string[]>();

  protected photos(trip: Trip): string[] {
    if (trip.photoUrls.length > 0) {
      return trip.photoUrls;
    }
    let photos = this.photosByTripId.get(trip.id);
    if (!photos) {
      photos = tripPhotoUrls(trip.id, 4);
      this.photosByTripId.set(trip.id, photos);
    }
    return photos;
  }
}
