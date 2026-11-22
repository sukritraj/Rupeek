package com.rupeek.hotel.support;

import com.rupeek.hotel.booking.BookingRepository;
import com.rupeek.hotel.booking.BookingRequest;
import com.rupeek.hotel.booking.BookingService;
import com.rupeek.hotel.booking.BookingSettings;
import com.rupeek.hotel.booking.GuestDetails;
import com.rupeek.hotel.booking.InMemoryBookingRepository;
import com.rupeek.hotel.booking.StayPolicy;
import com.rupeek.hotel.cancellation.CancellationPolicy;
import com.rupeek.hotel.cancellation.CancellationService;
import com.rupeek.hotel.cancellation.TieredCancellationPolicy;
import com.rupeek.hotel.cancellation.TieredCancellationPolicy.Tier;
import com.rupeek.hotel.common.DateRange;
import com.rupeek.hotel.common.KeyedLocks;
import com.rupeek.hotel.common.Money;
import com.rupeek.hotel.inventory.InMemoryInventoryLedger;
import com.rupeek.hotel.inventory.InventoryLedger;
import com.rupeek.hotel.payment.InMemoryPaymentRepository;
import com.rupeek.hotel.payment.MockPaymentGateway;
import com.rupeek.hotel.payment.PaymentCommand;
import com.rupeek.hotel.payment.PaymentGateway;
import com.rupeek.hotel.payment.PaymentMethodRegistry;
import com.rupeek.hotel.payment.PaymentRepository;
import com.rupeek.hotel.payment.PaymentService;
import com.rupeek.hotel.payment.method.CardPayment;
import com.rupeek.hotel.payment.method.UpiPayment;
import com.rupeek.hotel.payment.method.WalletPayment;
import com.rupeek.hotel.pricing.FlatNightlyPricing;
import com.rupeek.hotel.pricing.PricingStrategy;
import com.rupeek.hotel.property.InMemoryOwnerRepository;
import com.rupeek.hotel.property.InMemoryPropertyRepository;
import com.rupeek.hotel.property.Owner;
import com.rupeek.hotel.property.OwnerService;
import com.rupeek.hotel.property.Property;
import com.rupeek.hotel.property.PropertyDraft;
import com.rupeek.hotel.property.PropertyDraft.RoomTypeDraft;
import com.rupeek.hotel.property.PropertyRepository;
import com.rupeek.hotel.search.SearchService;
import com.rupeek.hotel.search.filter.AmenitiesFilter;
import com.rupeek.hotel.search.filter.PriceRangeFilter;
import com.rupeek.hotel.search.filter.StarRatingFilter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Hand-wired object graph (no Spring) so unit tests stay fast and read top to bottom. */
public class TestWorld {

    // 2026-06-10 10:00 UTC
    public final MutableClock clock = new MutableClock(Instant.parse("2026-06-10T10:00:00Z"));
    public final PropertyRepository properties = new InMemoryPropertyRepository();
    public final BookingRepository bookings = new InMemoryBookingRepository();
    public final PaymentRepository payments = new InMemoryPaymentRepository();
    public final InventoryLedger inventory = new InMemoryInventoryLedger();
    public final KeyedLocks locks = new KeyedLocks();
    public final PricingStrategy pricing;
    public final OwnerService ownerService = new OwnerService(new InMemoryOwnerRepository(), properties);
    public final BookingService bookingService;
    public final PaymentService paymentService;
    public final CancellationService cancellationService;
    public final SearchService searchService;

    public TestWorld() {
        this(new FlatNightlyPricing());
    }

    public TestWorld(PricingStrategy pricing) {
        this.pricing = pricing;
        BookingSettings settings = new BookingSettings(15, 30);
        StayPolicy stayPolicy = new StayPolicy(clock, settings);
        bookingService = new BookingService(properties, bookings, inventory, pricing, stayPolicy, locks, settings, clock);

        PaymentMethodRegistry registry =
                new PaymentMethodRegistry(List.of(new CardPayment(clock), new UpiPayment(), new WalletPayment()));
        PaymentGateway gateway = new MockPaymentGateway();
        paymentService = new PaymentService(bookingService, bookings, payments, registry, gateway, locks, clock);

        CancellationPolicy policy = new TieredCancellationPolicy(List.of(new Tier(48, 100), new Tier(24, 50)), clock);
        cancellationService = new CancellationService(bookingService, bookings, paymentService, inventory, policy, locks, clock);

        searchService = new SearchService(properties, inventory, pricing, stayPolicy,
                List.of(new PriceRangeFilter(), new StarRatingFilter(), new AmenitiesFilter()));
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public DateRange stay(int startInDays, int nights) {
        return new DateRange(today().plusDays(startInDays), today().plusDays(startInDays + nights));
    }

    /** One property with a single room type of the given capacity and stock, priced at 5000/night. */
    public Property propertyWithRooms(int totalRooms) {
        Owner owner = ownerService.register("Test Owner", "owner@test.example", List.of(draft("Test Inn", "Pune", "Kothrud", 3,
                Set.of("wifi"), new RoomTypeDraft("Standard", 2, totalRooms, Money.of(5000), Set.of()))));
        return ownerService.propertiesOf(owner.id()).get(0);
    }

    public static PropertyDraft draft(String name, String city, String locality, int stars, Set<String> amenities,
                                      RoomTypeDraft... rooms) {
        return new PropertyDraft(name, city, locality, stars, amenities, List.of(rooms));
    }

    public BookingRequest bookingRequest(Property property, DateRange stay, int guests) {
        return new BookingRequest(property.id(), property.roomTypes().get(0).id(), stay, guests,
                new GuestDetails("Guest One", "guest@test.example"));
    }

    public PaymentCommand upi(String bookingId, String vpa, String idempotencyKey) {
        return new PaymentCommand(bookingId, "UPI", Map.of("vpa", vpa), idempotencyKey);
    }
}
