# Agency Voyage

A group-travel booking platform: customers pool together on a trip, and the price per
seat drops in tiers as more people join. A group is confirmed only if it reaches the
trip's minimum participation before its booking deadline - otherwise it's cancelled.

Built with **Spring Boot 4.1 / Java 25** (hexagonal/clean architecture) on the backend,
**Angular 22** (standalone components, signals, Angular Material) on the frontend, and
**Kafka** for the event flow between joining a group and reacting to it.

## Contents

- [Architecture](#architecture)
- [Design](#design)
- [Business rules](#business-rules)
- [Running it locally](#running-it-locally)
- [API](#api)
- [Testing](#testing)
- [CI](#ci)
- [Git workflow](#git-workflow)
- [Simplifications and next steps](#simplifications-and-next-steps)

## Architecture

```
backend/
├── domain/          pure Java, zero framework dependencies
├── application/      use cases + ports, depends only on domain (also framework-free)
├── infrastructure/    adapters: JPA/Postgres, Kafka, scheduler - implements the ports
└── web/               the runnable Spring Boot app: REST controllers, bean wiring
frontend/              Angular app (standalone components, signals)
```

This is hexagonal architecture: `domain` has no dependencies at all. `application`
defines the use cases (`CreateGroupBookingUseCase`, `JoinGroupBookingUseCase`, ...) and
the ports they need (`TripRepository`, `GroupBookingEventPublisher`, ...) but never
imports Spring - its services are plain classes, constructor-injected. `infrastructure`
implements those ports against real technology (Postgres via Spring Data JPA, Kafka via
Spring Kafka). `web` is the only module that wires everything together
(`UseCaseWiringConfig`) and exposes it over HTTP.

**Domain model:**
- `Trip` - a travel package: destination, dates, min/max participants, booking
  deadline, and a `PricingSchedule` (base price + tiers that unlock as the group grows).
- `GroupBooking` - one group pooling on a trip. Captures the trip's min/max/deadline/
  pricing *at the moment it opens*, so later changes to the trip catalog don't
  retroactively affect an in-flight group. Owns the business rules: `join` (rejects a
  full group, a past deadline, or a closed booking) and `finalizeBooking` (confirms if
  the minimum was reached by the deadline, cancels otherwise).

**Event flow:** every join (including the creator's) publishes a
`ParticipantJoinedEvent` to Kafka, every leave a `ParticipantLeftEvent`, every
finalization a `GroupBookingFinalizedEvent`. A `GroupBookingFinalizationScheduler`
periodically finds `OPEN` bookings past their deadline and finalizes them. Three
independent consumer groups read these topics: a `NotificationKafkaListener` that logs
them - see [Simplifications](#simplifications-and-next-steps) for why that isn't its
own service - a `GroupBookingKafkaBridge` that fans a "this booking changed" ping out
over SSE (`GroupBookingEventBroadcaster`) to every browser tab watching that booking so
`GroupBookingDetail` updates live instead of waiting for its next poll, and an
`AuditTrailKafkaListener` that persists every event as one row in an `audit_event`
table - the group booking's full history, queryable via
`GET /api/group-bookings/{bookingId}/audit-trail` and shown as a timeline on the
booking page. Being built from the same events the other two consumers already read is
the point: the audit trail costs nothing extra to keep in sync, and a slow or failing
consumer on one topic never blocks the others, since each has its own consumer group.
When a departure promotes someone off the waitlist, the promotion itself publishes a
plain `ParticipantJoinedEvent` (not a separate event/topic) - to every existing
consumer it's indistinguishable from an ordinary join, so notifications, the audit
trail, and the live SSE update all pick it up for free.

## Design

The frontend is built on **Angular Material** (`@angular/material`, Material 3 /
`mat.theme()`) rather than hand-rolled components, but skinned with a custom light
"summer at the coast" palette (`primary: $azure-palette`, `tertiary: $orange-palette`,
`theme-type: light` in `src/styles.scss`) and type pairing (Sora for headings, Inter for
body) instead of stock Material blue/Roboto - a vivid ocean/sky azure paired with a
warm sunset-orange accent, the classic beach-holiday blue/orange pairing, picked for a
bright, sunny "summer travel" feel (an earlier dark "aurora" look, cycled through a
couple of color pairings including this same blue/orange split before, was dropped for
reading moody rather than inviting - and the warm-orange-primary variant that followed
it has now swapped back to blue-primary per direct design feedback). Material supplies
the system (ripple, elevation, motion, the M3 color/typography token system via
`--mat-sys-*` CSS variables), the custom theme and a handful of shared primitives
supply the look:

- A slow-drifting animated gradient mesh behind every page (`body`'s
  `aurora-drift` keyframe), built from the theme's own generated colors via
  `color-mix()` so it stays in harmony with whatever palette is active. It sits on its
  own crisp sky-blue-white base (`--page-base`, not the flatter `--mat-sys-surface`)
  with each color bloom kept small and low-opacity so they read as a soft breezy wash
  rather than hurting text contrast.
- A `.glass-panel`/`glass-panel` Sass mixin (`src/styles/_mixins.scss`, `@use`d via
  `stylePreprocessorOptions.includePaths` in `angular.json`) for the frosted,
  backdrop-blurred cards used across every page, with a soft cool-toned (navy-blue)
  baseline drop shadow so they read as elevated against `--page-base` even before any
  hover/focus state.
- A `.gradient-text` mixin/utility for small-to-medium gradient-clipped text (price
  figures, the nav brand, filled buttons) - deliberately **not** used for large bold
  headings, where `background-clip: text` rendered a visible double-stroke artifact in
  testing; big headlines use the `.accent-heading` utility (solid color + glow)
  instead. The underlying `--gradient-aurora` stays within the primary color's own
  tones (its lighter shade through to the much more saturated
  `--mat-sys-inverse-primary`) rather than crossing into the tertiary accent -
  blending two different hues in one gradient reliably drifts through a muddy
  in-between on the way (tried with violet/orange, then blue/yellow - both produced
  it), so tertiary is kept to small, solid, never-blended accents instead (status
  dots, badges).
- Staggered entrance animations (`fade-in-up` + per-card `animation-delay`) on the trip
  cards, a pulsing glow on an `OPEN` status badge, and glowing hover/focus states
  throughout, all layered on top of Material's own ripple and elevation.
- A full-bleed, cinematic homepage hero: a slow-crossfading photo backdrop (the
  existing `ImageCarousel`, reused with `pointer-events: none` so its own nav/dots
  stay inert as ambient background) behind a dark bottom-weighted gradient scrim, a
  huge bold display headline, and a pill-shaped search bar that floats half over the
  hero's bottom edge - the classic Airbnb/booking-site "start here" focal point,
  replacing the earlier flat-color hero with a decorative plane icon. The bar is a
  true frosted-glass panel (low-opacity white + heavy blur, not the near-solid
  `.glass-panel` used for ordinary content cards) so the photo keeps showing through
  it, and a gradient `Search` button smooth-scrolls to the results. Its two fields
  are plain hand-built pills (a native `<input>` in a flex row), not skinned
  `mat-form-field`s - that was tried first by overriding the MDC outline segments,
  but Material's "outline" appearance reserves internal layout space for a floating
  label that kept landing the prefix icon and input text on different baselines no
  matter how the outline chrome was re-skinned. A plain flex row sidesteps that
  entirely; `MatDatepickerModule`'s `[matDatepicker]` directive still works fine on a
  bare `<input>` without a `mat-form-field` ancestor, so the date field keeps full
  picker functionality.
- A bento-style trip gallery (`trip-list`'s `.trip-grid`): a repeating 4-across CSS
  Grid where every fourth card spans the full row as a featured banner and the rest
  pair up at half width, instead of a uniform card grid - breaks visual monotony
  without hardcoding which trips get the "featured" treatment, so it scales to any
  list length. Collapses to a single column below 720px.
- Responsive down to a 375px phone throughout: the header's `mat-toolbar` wraps onto
  a second row instead of clipping once its content no longer fits one line (its
  fixed Material height is overridden to `auto` below 760px for this), with the
  purely-decorative "Hi, {name}" greeting hidden first since it's the least essential
  piece; the admin dashboard's add/edit-trip form drops from two fixed columns to one
  below 560px (two columns left each field only ~130px - too cramped to use); and the
  floating chat widget's panel width is clamped with `min(320px, 100%)` plus
  side-anchored positioning below 420px so it can never overflow a narrow viewport;
  and each hotel-list row in the admin dashboard's "manage hotels" panel (name,
  description, and two action buttons side by side) stacks the text above
  full-width buttons below 560px instead of squeezing the description into an
  unreadable ragged sliver next to them.

### Language switching (English / French)

A lightweight, home-grown `I18nService` (`frontend/src/app/core/i18n.service.ts`) -
not Angular's built-in i18n package, which compiles a separate bundle per locale and
can't switch at runtime - holds the active language as a signal, two flat
key→string dictionaries (`core/i18n/en.ts`, `core/i18n/fr.ts`), and a `t(key, params?)`
lookup method with `{{param}}` interpolation. Components call `i18n.t('some.key')`
directly in their templates (no custom pipe); since `t()` reads the `lang` signal
internally, any OnPush component whose template calls it is automatically
re-rendered the moment the language changes, without needing `markForCheck()`
anywhere. The choice is persisted to `localStorage` and a 🇬🇧/🇫🇷 flag pair in the
header toolbar (`app.html`) switches it instantly, no page reload. Currently wired up
for the header nav/footer, the trip list, login/register, and the chat widget - see
Simplifications for what isn't translated yet.

## Business rules

Reframing the kata's original group-purchase stories for trips:

- A customer can create a group booking for a trip, or join an existing one, as long as
  the trip's booking deadline hasn't passed and the group isn't full. Joining sends the
  joiner a confirmation email (participant count and price per seat at the moment they
  joined).
- The price per seat drops as the group crosses each trip's price-tier thresholds -
  visible live to everyone already in the group.
- Once a group is full, new customers can join its waitlist instead. If a participant
  leaves before the deadline, the longest-waiting person on the waitlist is
  automatically promoted into the freed seat.
- Every participant can share an invite link for their group. A friend who joins
  through it gets $50 off their seat, and so does the person who invited them -
  discounts stack if someone refers several friends.
- Once a group's deadline passes: if it reached the trip's minimum participants, the
  trip is confirmed at whatever price tier the final count landed on; otherwise it's
  cancelled.
- Once a group booking is `CONFIRMED`, any logged-in user can record a hotel
  reservation reference for it (`PENDING`), then confirm that reservation
  (`CONFIRMED`) - confirming emails every participant their confirmation, and
  requesting emails them too, letting them know a reservation is pending.
- Any logged-in user can chat with the admin via a floating chat widget available on
  every page. Each customer has one ongoing thread with the admin (`ContactMessage`
  rows share a `conversationUserId` - always the customer's id - while
  `authorUserId`/`fromAdmin` track who actually wrote each message); a customer can
  only read and send into their own thread. An admin instead sees a list of every open
  conversation, newest activity first, and can open any one of them to reply - only an
  admin may list every conversation or reply as the admin (`403` otherwise). The widget
  polls every 5s while open to pick up the other side's replies; there is no AI
  involved, it is a plain human-to-admin messaging UI.
- An admin can create, edit and delete trips in the catalog (destination, dates,
  participant limits, booking deadline, pricing tiers) and curate a hotel catalog per
  trip (name, description, photo URLs, amenities - add, edit, delete) from a dedicated
  `/admin` dashboard - trips and hotels are visible to everyone browsing, but only an
  admin can change either. Amenities are a free-text tag list (e.g. "Restaurant",
  "Pool", "Free Wi-Fi") rather than a fixed enum, entered one per line the same way
  photo URLs already are, and shown as small pill badges on both the admin's hotel
  list and the public trip-detail page. Deleting a trip or hotel asks for confirmation
  first and can't be undone; deleting a trip doesn't touch any group bookings already
  made for it (no foreign-key link from booking to trip - see Simplifications).
  Logging in as an admin goes straight to the dashboard instead of the trip list.
- Any logged-in user can rate and review a hotel (1-5 stars, plus an optional text
  comment) on the trip-detail page, at most once per hotel - a repeat submission edits
  their existing review instead of creating a second one (enforced by a `(hotel_id,
  author_user_id)` unique constraint, not just an application-layer check). The
  author can edit or delete their own review; an admin can delete anyone's. Each
  hotel shows its average rating and review count as a star row, with every
  individual review (author, stars, date, comment) listed underneath - this is where
  "the client can see the hotel's score" before picking where to stay.
- The trip catalog can be searched by name/description and filtered to trips running on
  a chosen date (i.e. that date falls within the trip's departure-return window); both
  filters combine and update the list live as you type or pick a date.

## Running it locally

Requires Docker, Java 25, and Node 24+.

```bash
# 1. Postgres + Kafka + Kafka UI
docker compose up -d

# 2. Backend (http://localhost:8080) - seeds a few sample trips on first run
cd backend
./mvnw -pl web spring-boot:run

# 3. Frontend (http://localhost:4200)
cd frontend
npm install
npm start
```

Kafka UI is at `http://localhost:8085` if you want to watch the topics
(`group-booking.participant-joined`, `group-booking.finalized`) fill up as you use the app.

The frontend's API base URL is hardcoded to `http://localhost:8080` in
`frontend/src/app/core/api-config.ts` - there's no build-time environment config yet.

## API

| Method | Path                                     | Auth | Description                          |
|--------|-------------------------------------------|------|---------------------------------------|
| POST   | `/api/auth/register`                      | -    | Create an account (`{email, password, displayName}`) → `{token, user}` |
| POST   | `/api/auth/login`                         | -    | Log in (`{email, password}`) → `{token, user}` |
| GET    | `/api/trips`                              | -    | List the trip catalog                 |
| GET    | `/api/trips/{tripId}`                     | -    | Get one trip                          |
| POST   | `/api/trips`                              | admin only | Add a trip to the catalog (same body shape as `PUT`) - `403` for a non-admin |
| PUT    | `/api/trips/{tripId}`                     | admin only | Edit a trip (`{destination, description, departureDate, returnDate, minParticipants, maxParticipants, bookingDeadline, basePrice, priceTiers}`) - `403` for a non-admin, `404` if unknown |
| DELETE | `/api/trips/{tripId}`                     | admin only | Remove a trip from the catalog - `403` for a non-admin, `404` if unknown |
| POST   | `/api/trips/{tripId}/group-bookings`      | required | Start a group booking as the caller |
| POST   | `/api/group-bookings/{bookingId}/participants` | required | Join a group booking as the caller (`?ref={participantId}` credits a referral discount to both sides) |
| DELETE | `/api/group-bookings/{bookingId}/participants/me` | required | Leave a group booking as the caller (auto-promotes the next waitlisted person, if any) |
| POST   | `/api/group-bookings/{bookingId}/waitlist` | required | Join the waitlist - only once the group is full |
| DELETE | `/api/group-bookings/{bookingId}/waitlist/me` | required | Leave the waitlist without waiting for a seat |
| GET    | `/api/group-bookings/{bookingId}`         | -    | Get a group booking's current state (includes `myParticipantId`/`myWaitlistEntryId`/`myPricePerSeat` if a valid token is sent) |
| GET    | `/api/group-bookings/{bookingId}/events`  | -    | SSE stream: a ping each time the booking changes |
| GET    | `/api/group-bookings/{bookingId}/audit-trail` | -    | Full history (joins/leaves/finalization), oldest first |
| POST   | `/api/group-bookings/{bookingId}/hotel-reservation` | required | Record a hotel reservation reference (`{reference}`) for a `CONFIRMED` booking → `PENDING`. Emails every participant |
| POST   | `/api/group-bookings/{bookingId}/hotel-reservation/confirm` | required | Confirm a `PENDING` reservation → `CONFIRMED`. Emails every participant |
| POST   | `/api/contact-messages`                   | required | Send a message as the caller, starting or continuing their own thread with the admin (`{message}`) |
| POST   | `/api/contact-messages/reply`             | admin only | Reply into a customer's thread (`{conversationUserId, message}`) - `403` for a non-admin |
| GET    | `/api/contact-messages/conversations/{userId}` | required | Get one thread, oldest first - the thread's own customer or any admin, `403` otherwise |
| GET    | `/api/contact-messages`                   | admin only | List every message across every conversation, newest first - `403` for a non-admin |
| GET    | `/api/trips/{tripId}/hotels`              | -    | List the hotel catalog for a trip |
| POST   | `/api/trips/{tripId}/hotels`              | admin only | Add a hotel to a trip's catalog (`{name, description, photoUrls, amenities}`) - `403` for a non-admin |
| PUT    | `/api/trips/{tripId}/hotels/{hotelId}`    | admin only | Edit a hotel (same body as `POST`) - `403` for a non-admin, `404` if unknown |
| DELETE | `/api/trips/{tripId}/hotels/{hotelId}`    | admin only | Remove a hotel from a trip's catalog - `403` for a non-admin, `404` if unknown |
| POST   | `/api/hotels/{hotelId}/reviews`           | required | Rate and review a hotel (`{rating, comment}`) - `409` if the caller already reviewed this hotel |
| GET    | `/api/hotels/{hotelId}/reviews`           | -    | List a hotel's reviews, newest first |
| PUT    | `/api/hotels/{hotelId}/reviews/{reviewId}` | required | Edit your own review (same body as `POST`) - `403` if not the author, `404` if unknown |
| DELETE | `/api/hotels/{hotelId}/reviews/{reviewId}` | required | Delete a review - the author or an admin only, `403` otherwise, `404` if unknown |

Authenticated requests send `Authorization: Bearer <token>`, a JWT (HS256) returned by
register/login. Its secret and expiration are configured via
`agency-voyage.jwt.secret` / `agency-voyage.jwt.expiration-ms` in `application.yml`
(overridable with the `AGENCY_VOYAGE_JWT_SECRET` env var - the default is a dev-only
value, change it for anything beyond local use). The token also carries an `isAdmin`
claim, so admin status survives round-trips without a database lookup on every request.

There's no public admin-registration flow - `AdminUserSeeder` creates one admin account
on startup if it doesn't already exist (`admin@agencyvoyage.example` /
`admin12345` by default, overridable via `AGENCY_VOYAGE_ADMIN_EMAIL` /
`AGENCY_VOYAGE_ADMIN_PASSWORD` - dev-only credentials, change them for anything beyond
local use).

Errors: `401` for a missing/invalid token on a protected endpoint, `403` for an
authenticated but non-admin caller on an admin-only endpoint, `404` for an unknown
trip/booking, `409` for a domain rule violation (group full, deadline passed, already
finalized, already joined, email already registered, hotel reservation requested out of
order), `400` for validation failures.

## Testing

Each backend module has fast unit tests (`*Test.java`, run by `mvn test`) and, where
real infrastructure matters, integration tests (`*IT.java`, run by `mvn verify` via
Failsafe) against **Testcontainers** - real Postgres and Kafka, not mocks or H2.

```bash
cd backend
./mvnw test      # fast: domain rules, use-case logic (mocked ports), the scheduler
./mvnw verify     # + integration tests: real Postgres/Kafka via Testcontainers (needs Docker)
```

What's covered where:
- `domain` - pricing-tier resolution, every `GroupBooking` state transition.
- `application` - use-case services against fakes/Mockito for their ports.
- `infrastructure` - JPA repository adapters and the Kafka producer, against real
  Testcontainers Postgres/Kafka.
- `web` - `@WebMvcTest` slices for validation/error-mapping, plus a full
  `@SpringBootTest` that drives the real REST API against Testcontainers Postgres +
  Kafka end to end (create/join across a price tier, persisted state, the Kafka event
  actually landing on the topic).

Frontend:

```bash
cd frontend
npm test          # Vitest, component tests with HttpTestingController
```

## CI

Two GitHub Actions workflows (`.github/workflows/`), each scoped to its own directory
so unrelated changes don't trigger a run:
- `backend-ci.yml` - `./mvnw verify` (unit + integration tests; GitHub-hosted runners
  have Docker, so Testcontainers works without extra setup).
- `frontend-ci.yml` - `ng test` + `ng build`.

## Git workflow

One feature branch per feature, pushed to `origin`. `main` is never pushed to or merged
into by this work - branches are merged in by hand, in order:

`project-scaffold` → `domain-model` → `application-use-cases` → `persistence-postgres`
→ `kafka-events` → `rest-api` → `frontend-trip-catalog` → `frontend-group-booking` →
`ci-pipelines` → `live-price-updates` → `leave-group-booking` → `authentication` →
`ui-carousels` → `audit-trail` → `waitlist` → `referral-discounts` → `fancy-ui-redesign`
→ `hotel-reservation-and-contact-admin` → `admin-hotel-catalog` →
`join-confirmation-email` → `dark-theme-polish-and-search` → `blue-yellow-theme` →
`dark-mode-redesign` → `light-theme` → `admin-dashboard` → `delete-trip-and-hotel` → `admin-chat-widget`
→ `chat-widget-show-customer-name` → `hero-plane-background` → `i18n-en-fr`
→ `admin-dashboard-i18n-and-polish` → `i18n-trip-and-booking-detail`
→ `cinematic-editorial-homepage` → `fancy-hero-search-bar` → `custom-search-pill`
→ `admin-dashboard-spacing-polish` → `summer-blue-theme` → `responsive-polish`
→ `admin-hotel-list-responsive` → `hotel-amenities` → `hotel-reviews`

## Simplifications and next steps

Documented deliberately, not accidentally missed:

- **`NotificationKafkaListener` lives in the same deployable as everything else.** In a
  real system this would be its own service, consuming the same topics to actually
  notify customers. Kept in-process here to demonstrate the event flow without standing
  up a second deployable for an MVP pass.
- **Live updates hold their state in memory, in the one deployable.**
  `GroupBookingEventBroadcaster` keeps its SSE subscribers in a plain in-memory map on
  the `web` instance that received the connection. That's fine for one instance; running
  several behind a load balancer would need either sticky sessions or moving the fan-out
  itself onto Kafka (e.g. each instance's bridge re-publishing to a per-connection
  topic, or a shared pub/sub layer) so a subscriber connected to instance A still hears
  about an event consumed by instance B. `GroupBookingDetail` also keeps a 20s fallback
  poll as a backstop in case an SSE connection drops.
- **Trips are seed data, not admin-managed.** `TripCatalogSeeder` inserts a handful of
  sample trips on first startup; there's no create/edit flow for the catalog itself.
- **Auth is email/password + JWT, no refresh tokens.** `register`/`login` issue a
  single long-lived (24h) JWT; there's no refresh flow or revocation - logging out just
  drops the token client-side. `User` stays a pure identity in `domain` (id, email,
  display name); the password hash lives only in `infrastructure`
  (`UserJpaEntity`/`BCryptPasswordHasher`), never touching the domain or application
  layers. "Which participant is me" is now computed server-side on every response
  (`GroupBookingResponse.myParticipantId`) from the caller's authenticated `UserId`,
  replacing the earlier `localStorage`-based heuristic.
- **Trip photos are stand-in placeholders, not real destination photography.** The
  frontend's `tripPhotoUrls` generates a deterministic picsum.photos set per trip id
  (same trip always gets the same photos) so the carousels have something to show;
  there's no real photo library or upload flow wired up.
- **English/French translation covers every page except backend-returned error
  text.** The header nav/footer, trip list, login/register, chat widget, admin
  dashboard, trip detail, and group-booking detail (join/leave, waitlist, referrals,
  hotel reservation, audit-trail history) are all fully bilingual; only messages that
  come verbatim from the API (validation errors, `DomainException` messages, etc.) are
  still English-only, since translating those would mean localizing the backend
  itself. Adding a new frontend string to the dictionary is
  mechanical (add keys to both `core/i18n/en.ts` and `core/i18n/fr.ts`, call
  `i18n.t('key')` in the template) - just not done everywhere yet.
- **Hotel reviews aren't restricted to verified stays.** Any logged-in user can rate
  and review any hotel, with no check that they actually traveled with a confirmed
  group booking for that trip - a deliberate scope choice (Airbnb/Booking.com-style
  "verified stay only" reviews would need a new check against the traveler's booking
  history) rather than an oversight. The one-review-per-hotel rule is still fully
  enforced (a DB unique constraint, not just a UI nicety).
- **The audit trail has no replay/backfill path.** It's built purely from events
  consumed going forward; if `audit_event` were ever dropped or a booking existed
  before this feature shipped, its earlier history is gone rather than reconstructible
  from Kafka (the topics aren't retained indefinitely, and there's no snapshot to
  rebuild from). A production version would need either long topic retention plus a
  rebuild job, or to treat `audit_event` as the durable store from day one.
- **The waitlist has no expiry or reservation window.** A promoted waitlist entry
  becomes a full participant immediately and permanently - there's no "you have 10
  minutes to confirm your seat" step, so a promoted user who never comes back still
  occupies the seat until someone finalizes or they leave themselves.
- **The referral discount is a flat amount, not configurable per trip, and doesn't
  track fraud.** `GroupBooking.REFERRAL_DISCOUNT_PER_CREDIT` is a single hardcoded $50
  for every trip; there's also no limit on how many friends one person can refer
  (discounts stack indefinitely, floored at a $0 seat) and no check for collusion (two
  accounts referring each other back and forth). Fine for an MVP demonstrating the
  mechanic, not for production.
- **Hotel reservations aren't gated by the admin role.** Confirming a hotel
  reservation is open to any logged-in user, not just admins - this predates `isAdmin`
  and was deliberately kept as a simplification. The contact-message/chat-widget
  endpoints *are* fully admin-gated now (a customer only sees their own thread, only an
  admin can list every conversation or reply into one), as is the hotel *catalog*
  (`POST /api/trips/{tripId}/hotels`).
- **All transactional emails are simulated, like Kafka notifications.**
  `LoggingEmailSender` logs what would be sent (join confirmation, hotel reservation
  requested/confirmed) instead of calling a real provider - the same simplification
  already made for `NotificationKafkaListener`, not wired to SES/SendGrid/etc.
- **Hotel catalog photos are plain URLs, not an upload flow.** Same simplification as
  trip photos: an admin pastes image URLs when adding a hotel - no file upload or
  object storage.
- **Deleting a trip doesn't check for existing group bookings.** There's no
  foreign-key link from `group_booking` to `trip` (a booking captures the trip's
  min/max/deadline/pricing at the moment it opens, not a live reference - see
  `GroupBooking`'s own docs), so deleting a trip that already has bookings leaves them
  pointing at a now-missing trip id rather than being blocked or cascade-deleted. Fine
  for an MVP; a production version would need to either block the delete when bookings
  exist or archive the trip instead of removing it.
- **Further bonus ideas from the original brainstorm** not built here: multi-currency
  pricing.
