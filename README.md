# Hotel Booking Service

Backend for a hotel booking platform: owners onboard properties, guests search, book, pay and cancel.
Java 17, Spring Boot 3.3, in-memory persistence behind repository interfaces.

## Build and run

```bash
mvn test                              # 52 tests (unit + one end-to-end HTTP test)
mvn spring-boot:run                   # or: mvn package && java -jar target/hotel-booking-1.0.0.jar
./scripts/demo.sh                     # search -> book -> pay (decline, then UPI) -> cancel, against the running service
```

* Service: http://localhost:8080
* Swagger UI: http://localhost:8080/swagger-ui.html
* On startup a chain (3 properties, Bengaluru/Mumbai) and a standalone guest house are seeded.
  Turn it off with `hotel.seed.enabled=false`.

## API

| Operation | Endpoint |
|---|---|
| Onboard owner + properties (1 = standalone, n = chain) | `POST /api/owners` |
| Add a property to an existing owner | `POST /api/owners/{ownerId}/properties` |
| Get owner / property | `GET /api/owners/{id}`, `GET /api/properties/{id}` |
| Search | `GET /api/properties/search?city=&locality=&checkIn=&checkOut=&guests=&minPrice=&maxPrice=&minStars=&amenities=wifi,pool` |
| Book | `POST /api/bookings` |
| Pay | `POST /api/bookings/{id}/payments` (optional `Idempotency-Key` header) |
| Cancel | `POST /api/bookings/{id}/cancel` |
| Get booking | `GET /api/bookings/{id}` |

Payment bodies: `{"method":"CARD","details":{"cardNumber":"...","expiry":"MM/YY"}}`,
`{"method":"UPI","details":{"vpa":"name@bank"}}`,
`{"method":"WALLET","details":{"provider":"paytm","walletId":"..."}}`.
The mock gateway declines any credential ending in `0000` or containing `fail`.

Errors come back as `{timestamp, status, error, message}`: 400 validation, 404 unknown id,
409 sold out / invalid state, 502 provider failure.

## Design

Packages are by feature (`property`, `search`, `inventory`, `pricing`, `booking`, `payment`,
`cancellation`) with `api` as a thin REST layer on top. Domain classes have no Spring or
persistence annotations; services depend on interfaces.

| Seam | Interface | Implementations | To extend |
|---|---|---|---|
| Persistence | `*Repository` | `InMemory*Repository` | write a JPA implementation |
| Inventory | `InventoryLedger` | `InMemoryInventoryLedger` | Redis / DB row locks |
| Search filters | `OfferFilterFactory` | price range, star rating, amenities | add one `@Component`; search code is untouched |
| Payment methods | `PaymentMethod` | card, UPI, wallet | add one `@Component` |
| Payment provider | `PaymentGateway` | `MockPaymentGateway` | real gateway client |
| Pricing | `PricingStrategy` | flat nightly, weekend surcharge | new bean, pick with `hotel.pricing.strategy` |
| Cancellation | `CancellationPolicy` | `TieredCancellationPolicy` | new bean in `HotelConfig` |

Decisions worth knowing about:

* **Single property = chain of one.** There is one onboarding path: an `Owner` holds a list of
  properties. A standalone hotel is a request with one property in the list.
* **Filters are open-ended.** The query carries typed core criteria plus a `Map` of raw filter
  params. Each `OfferFilterFactory` reads the keys it understands and returns a predicate (or
  nothing). `SearchService` ANDs whatever comes back, so a new filter never touches existing code.
* **Booking lifecycle.** `PENDING_PAYMENT -> CONFIRMED -> CANCELLED`, plus `PENDING_PAYMENT -> EXPIRED`
  and `PENDING_PAYMENT -> CANCELLED`. Transitions are enforced inside `Booking` /
  `BookingStatus`, so no service can skip a state.
* **Payment drives the booking.** Success confirms it. A decline leaves it pending so the guest can
  retry with another method until the hold expires.
* **Inventory and concurrency.** Inventory is a ledger of rooms held per room type per night.
  `reserve` checks every night and increments them under a per-room-type lock, so two guests racing
  for the last room cannot both win and different room types never contend. Payment and cancellation
  take a per-booking lock, so double-pay and double-cancel are serialised (both covered by
  concurrency tests).
* **Holds expire.** A booking holds a room for 15 minutes (`hotel.booking.hold-minutes`). A scheduled
  sweep releases stale holds, and paying on a stale hold also releases it and fails.
* **Idempotent payments.** The same `Idempotency-Key` returns the original payment instead of
  charging again; reusing a key on another booking is a 409.
* **Refunds.** The default policy is 100% at 48h+ before check-in (14:00), 50% at 24h+, nothing
  after. The money movement happens before any state change, so a failing gateway leaves the
  booking untouched.
* **Price is locked at booking time**, not recomputed on payment.

## Assumptions

* One room per booking; guests must not exceed the room type's capacity.
* Currency is INR; amounts are 2-decimal `BigDecimal` wrapped in `Money`.
* Search price filters compare the per-night price; amenity filters match property **or** room amenities.
* Stays are at most 30 nights and cannot start in the past. Check-out day is not an occupied night.
* A booking cannot be cancelled once its stay is over.
* Auth, UI, and real payment integration are out of scope as per the brief.

## With more time

* Persist to a database, with a unique/conditional update on inventory instead of in-process locks, so it scales past one instance.
* Multiple rooms per booking and owner-managed inventory changes (blocking dates, price overrides per date).
* Pagination and sorting options on search, plus an index by city instead of a scan.
* Publish booking events (confirmed / cancelled) instead of calling collaborators directly.
* Make the cancellation tiers configurable per property and add payment webhooks / async confirmation.
* Evict idle entries from `KeyedLocks`.
