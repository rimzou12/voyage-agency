import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../core/auth.service';
import { CloudinaryUploadService } from '../core/cloudinary-upload.service';
import { I18nService } from '../core/i18n.service';
import { apiErrorMessage } from '../core/group-booking.service';
import { HotelService } from '../core/hotel.service';
import { TripInput, TripService } from '../core/trip.service';
import { Hotel, PriceTier, Trip } from '../core/models';

interface TripFormState {
  destination: string;
  description: string;
  departureDate: string;
  returnDate: string;
  minParticipants: number;
  maxParticipants: number;
  bookingDeadline: string;
  basePrice: number;
  priceTiers: string;
}

const BLANK_TRIP_FORM: TripFormState = {
  destination: '',
  description: '',
  departureDate: '',
  returnDate: '',
  minParticipants: 2,
  maxParticipants: 10,
  bookingDeadline: '',
  basePrice: 0,
  priceTiers: '',
};

interface HotelFormState {
  name: string;
  description: string;
  photoUrls: string;
  amenities: string;
}

const BLANK_HOTEL_FORM: HotelFormState = { name: '', description: '', photoUrls: '', amenities: '' };

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './admin-dashboard.html',
  styleUrl: './admin-dashboard.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AdminDashboard implements OnInit {
  private readonly router = inject(Router);
  private readonly tripService = inject(TripService);
  private readonly hotelService = inject(HotelService);
  private readonly snackBar = inject(MatSnackBar);
  protected readonly cloudinaryUpload = inject(CloudinaryUploadService);
  protected readonly auth = inject(AuthService);
  protected readonly i18n = inject(I18nService);

  protected readonly trips = signal<Trip[]>([]);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  // Create/edit trip form. editingTripId null + showTripForm true => creating;
  // editingTripId set => editing that trip. Only one form is shown at a time.
  protected readonly showTripForm = signal(false);
  protected readonly editingTripId = signal<string | null>(null);
  protected readonly tripForm = signal<TripFormState>({ ...BLANK_TRIP_FORM });
  protected readonly savingTrip = signal(false);
  protected readonly tripFormError = signal<string | null>(null);

  protected readonly deletingTripId = signal<string | null>(null);
  protected readonly deleteTripError = signal<string | null>(null);

  // Hotel management, scoped to at most one expanded trip at a time.
  protected readonly expandedTripId = signal<string | null>(null);
  protected readonly hotels = signal<Hotel[]>([]);
  protected readonly loadingHotels = signal(false);
  protected readonly editingHotelId = signal<string | null>(null);
  protected readonly hotelForm = signal<HotelFormState>({ ...BLANK_HOTEL_FORM });
  protected readonly savingHotel = signal(false);
  protected readonly hotelFormError = signal<string | null>(null);
  protected readonly deletingHotelId = signal<string | null>(null);
  protected readonly deleteHotelError = signal<string | null>(null);
  protected readonly uploadingHotelPhoto = signal(false);
  protected readonly hotelPhotoUploadError = signal<string | null>(null);

  ngOnInit(): void {
    if (!this.auth.currentUser()?.isAdmin) {
      this.router.navigateByUrl('/');
      return;
    }
    this.refreshTrips();
  }

  private showToast(message: string): void {
    this.snackBar.open(message, this.i18n.t('admin.dismiss'), { duration: 4000 });
  }

  private refreshTrips(): void {
    this.loading.set(true);
    this.tripService.listTrips().subscribe({
      next: (trips) => {
        this.trips.set(trips);
        this.loading.set(false);
      },
      error: () => {
        this.error.set(this.i18n.t('admin.loadTripsError'));
        this.loading.set(false);
      },
    });
  }

  // --- Trip create/edit ---

  protected startCreatingTrip(): void {
    this.editingTripId.set(null);
    this.tripForm.set({ ...BLANK_TRIP_FORM });
    this.tripFormError.set(null);
    this.showTripForm.set(true);
  }

  protected startEditingTrip(trip: Trip): void {
    this.editingTripId.set(trip.id);
    this.tripForm.set({
      destination: trip.destination,
      description: trip.description,
      departureDate: trip.departureDate,
      returnDate: trip.returnDate,
      minParticipants: trip.minParticipants,
      maxParticipants: trip.maxParticipants,
      bookingDeadline: toDatetimeLocal(trip.bookingDeadline),
      basePrice: trip.basePrice,
      priceTiers: trip.priceTiers.map((t) => `${t.minParticipants},${t.pricePerSeat}`).join('\n'),
    });
    this.tripFormError.set(null);
    this.showTripForm.set(true);
  }

  protected cancelTripForm(): void {
    this.showTripForm.set(false);
    this.editingTripId.set(null);
  }

  protected submitTripForm(): void {
    const form = this.tripForm();
    if (!form.destination.trim() || !form.departureDate || !form.returnDate || !form.bookingDeadline) {
      return;
    }
    const input: TripInput = {
      destination: form.destination.trim(),
      description: form.description.trim(),
      departureDate: form.departureDate,
      returnDate: form.returnDate,
      minParticipants: Number(form.minParticipants),
      maxParticipants: Number(form.maxParticipants),
      bookingDeadline: new Date(form.bookingDeadline).toISOString(),
      basePrice: Number(form.basePrice),
      priceTiers: parsePriceTiers(form.priceTiers),
    };

    this.savingTrip.set(true);
    this.tripFormError.set(null);
    const editingId = this.editingTripId();
    const request = editingId ? this.tripService.updateTrip(editingId, input) : this.tripService.createTrip(input);
    request.subscribe({
      next: () => {
        this.savingTrip.set(false);
        this.showTripForm.set(false);
        const toastKey = editingId ? 'admin.tripUpdatedToast' : 'admin.tripCreatedToast';
        this.editingTripId.set(null);
        this.refreshTrips();
        this.showToast(this.i18n.t(toastKey));
      },
      error: (err: HttpErrorResponse) => {
        this.savingTrip.set(false);
        this.tripFormError.set(apiErrorMessage(err, this.i18n.t('admin.saveTripError')));
      },
    });
  }

  protected deleteTrip(trip: Trip): void {
    if (!confirm(this.i18n.t('admin.deleteTripConfirm', { destination: trip.destination }))) {
      return;
    }
    this.deletingTripId.set(trip.id);
    this.deleteTripError.set(null);
    this.tripService.deleteTrip(trip.id).subscribe({
      next: () => {
        this.deletingTripId.set(null);
        if (this.expandedTripId() === trip.id) {
          this.expandedTripId.set(null);
        }
        if (this.editingTripId() === trip.id) {
          this.cancelTripForm();
        }
        this.refreshTrips();
      },
      error: (err: HttpErrorResponse) => {
        this.deletingTripId.set(null);
        this.deleteTripError.set(apiErrorMessage(err, this.i18n.t('admin.deleteTripError')));
      },
    });
  }

  // --- Hotel management ---

  protected toggleHotels(trip: Trip): void {
    if (this.expandedTripId() === trip.id) {
      this.expandedTripId.set(null);
      return;
    }
    this.expandedTripId.set(trip.id);
    this.editingHotelId.set(null);
    this.hotelForm.set({ ...BLANK_HOTEL_FORM });
    this.hotelFormError.set(null);
    this.refreshHotels(trip.id);
  }

  private refreshHotels(tripId: string): void {
    this.loadingHotels.set(true);
    this.hotelService.listHotels(tripId).subscribe({
      next: (hotels) => {
        this.hotels.set(hotels);
        this.loadingHotels.set(false);
      },
      error: () => this.loadingHotels.set(false),
    });
  }

  protected startAddingHotel(): void {
    this.editingHotelId.set(null);
    this.hotelForm.set({ ...BLANK_HOTEL_FORM });
    this.hotelFormError.set(null);
  }

  protected startEditingHotel(hotel: Hotel): void {
    this.editingHotelId.set(hotel.id);
    this.hotelForm.set({
      name: hotel.name,
      description: hotel.description,
      photoUrls: hotel.photoUrls.join('\n'),
      amenities: hotel.amenities.join('\n'),
    });
    this.hotelFormError.set(null);
  }

  protected cancelHotelForm(): void {
    this.editingHotelId.set(null);
    this.hotelForm.set({ ...BLANK_HOTEL_FORM });
  }

  protected uploadHotelPhoto(input: HTMLInputElement): void {
    const file = input.files?.[0];
    input.value = ''; // allow re-selecting the same file later
    if (!file) {
      return;
    }
    this.uploadingHotelPhoto.set(true);
    this.hotelPhotoUploadError.set(null);
    this.cloudinaryUpload.upload(file).subscribe({
      next: (url) => {
        this.uploadingHotelPhoto.set(false);
        const form = this.hotelForm();
        const photoUrls = form.photoUrls.trim().length > 0 ? `${form.photoUrls}\n${url}` : url;
        this.hotelForm.set({ ...form, photoUrls });
      },
      error: () => {
        this.uploadingHotelPhoto.set(false);
        this.hotelPhotoUploadError.set(this.i18n.t('admin.photoUploadError'));
      },
    });
  }

  protected submitHotelForm(): void {
    const tripId = this.expandedTripId();
    const form = this.hotelForm();
    if (!tripId || !form.name.trim() || !form.description.trim()) {
      return;
    }
    const photoUrls = form.photoUrls
      .split(/\r?\n/)
      .map((url) => url.trim())
      .filter((url) => url.length > 0);
    const amenities = form.amenities
      .split(/\r?\n/)
      .map((amenity) => amenity.trim())
      .filter((amenity) => amenity.length > 0);

    this.savingHotel.set(true);
    this.hotelFormError.set(null);
    const editingId = this.editingHotelId();
    const request = editingId
      ? this.hotelService.updateHotel(
          tripId,
          editingId,
          form.name.trim(),
          form.description.trim(),
          photoUrls,
          amenities,
        )
      : this.hotelService.addHotel(tripId, form.name.trim(), form.description.trim(), photoUrls, amenities);
    request.subscribe({
      next: () => {
        this.savingHotel.set(false);
        const toastKey = editingId ? 'admin.hotelUpdatedToast' : 'admin.hotelAddedToast';
        this.editingHotelId.set(null);
        this.hotelForm.set({ ...BLANK_HOTEL_FORM });
        this.refreshHotels(tripId);
        this.showToast(this.i18n.t(toastKey));
      },
      error: (err: HttpErrorResponse) => {
        this.savingHotel.set(false);
        this.hotelFormError.set(apiErrorMessage(err, this.i18n.t('admin.saveHotelError')));
      },
    });
  }

  protected deleteHotel(hotel: Hotel): void {
    const tripId = this.expandedTripId();
    if (!tripId || !confirm(this.i18n.t('admin.deleteHotelConfirm', { name: hotel.name }))) {
      return;
    }
    this.deletingHotelId.set(hotel.id);
    this.deleteHotelError.set(null);
    this.hotelService.deleteHotel(tripId, hotel.id).subscribe({
      next: () => {
        this.deletingHotelId.set(null);
        if (this.editingHotelId() === hotel.id) {
          this.cancelHotelForm();
        }
        this.refreshHotels(tripId);
      },
      error: (err: HttpErrorResponse) => {
        this.deletingHotelId.set(null);
        this.deleteHotelError.set(apiErrorMessage(err, this.i18n.t('admin.deleteHotelError')));
      },
    });
  }
}

function parsePriceTiers(raw: string): PriceTier[] {
  return raw
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line.length > 0)
    .map((line) => {
      const [minParticipants, pricePerSeat] = line.split(',').map((part) => Number(part.trim()));
      return { minParticipants, pricePerSeat };
    })
    .filter((tier) => Number.isFinite(tier.minParticipants) && Number.isFinite(tier.pricePerSeat));
}

/** Instant (ISO with offset) -> the local "YYYY-MM-DDTHH:mm" shape <input type="datetime-local"> expects. */
function toDatetimeLocal(instant: string): string {
  const date = new Date(instant);
  const pad = (n: number) => n.toString().padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}
