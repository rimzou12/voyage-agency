export interface PriceTier {
  minParticipants: number;
  pricePerSeat: number;
}

export interface Trip {
  id: string;
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

export type GroupBookingStatus = 'OPEN' | 'CONFIRMED' | 'CANCELLED';

export type HotelReservationStatus = 'NOT_REQUESTED' | 'PENDING' | 'CONFIRMED';

export interface Participant {
  id: string;
  customerName: string;
  joinedAt: string;
  /** The participant whose invite link this person joined through, or null for an organic join. */
  referredByParticipantId: string | null;
}

export interface WaitlistEntry {
  id: string;
  customerName: string;
  joinedAt: string;
}

export interface GroupBooking {
  id: string;
  tripId: string;
  status: GroupBookingStatus;
  participantCount: number;
  minParticipants: number;
  maxParticipants: number;
  currentPricePerSeat: number;
  deadline: string;
  priceTiers: PriceTier[];
  participants: Participant[];
  /** Which participant (if any) belongs to the caller, computed from the auth token. */
  myParticipantId: string | null;
  /** Oldest-waiting first. */
  waitlist: WaitlistEntry[];
  /** Which waitlist entry (if any) belongs to the caller, computed from the auth token. */
  myWaitlistEntryId: string | null;
  /** The caller's own price per seat, including any referral discount. Null unless the caller is a participant. */
  myPricePerSeat: number | null;
  hotelReservationStatus: HotelReservationStatus;
  /** Set once a reservation has been requested; null while NOT_REQUESTED. */
  hotelReservationReference: string | null;
}

export type AuditEventType = 'PARTICIPANT_JOINED' | 'PARTICIPANT_LEFT' | 'FINALIZED';

export interface AuditEvent {
  type: AuditEventType;
  participantId: string | null;
  customerName: string | null;
  participantCount: number;
  pricePerSeat: number;
  /** Only set for a FINALIZED entry. */
  status: GroupBookingStatus | null;
  occurredAt: string;
}

export interface User {
  id: string;
  email: string;
  displayName: string;
  isAdmin: boolean;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface Hotel {
  id: string;
  tripId: string;
  name: string;
  description: string;
  photoUrls: string[];
  amenities: string[];
}

export interface HotelReview {
  id: string;
  hotelId: string;
  authorId: string;
  authorName: string;
  rating: number;
  comment: string | null;
  reviewedAt: string;
}

export interface ContactMessage {
  id: string;
  conversationUserId: string;
  authorName: string;
  authorEmail: string;
  fromAdmin: boolean;
  message: string;
  sentAt: string;
}
