package com.rupeek.hotel;

import com.rupeek.hotel.common.Money;
import com.rupeek.hotel.common.exception.ValidationException;
import com.rupeek.hotel.property.PropertyDraft.RoomTypeDraft;
import com.rupeek.hotel.property.Property;
import com.rupeek.hotel.search.PropertySearchResult;
import com.rupeek.hotel.search.SearchQuery;
import com.rupeek.hotel.support.TestWorld;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchServiceTest {

    private final TestWorld world = new TestWorld();
    private Property grand;
    private Property budget;

    @BeforeEach
    void seed() {
        // one chain with two Bengaluru hotels and one in Mumbai
        var owner = world.ownerService.register("Chain", "chain@test.example", List.of(
                TestWorld.draft("Grand", "Bengaluru", "Indiranagar", 5, Set.of("wifi", "pool"),
                        new RoomTypeDraft("Deluxe", 2, 1, Money.of(9000), Set.of("breakfast")),
                        new RoomTypeDraft("Family", 4, 2, Money.of(15000), Set.of())),
                TestWorld.draft("Budget", "bengaluru", "Whitefield", 2, Set.of("WiFi"),
                        new RoomTypeDraft("Basic", 2, 5, Money.of(2000), Set.of())),
                TestWorld.draft("Bay", "Mumbai", "Bandra", 4, Set.of("wifi"),
                        new RoomTypeDraft("Sea View", 2, 3, Money.of(11000), Set.of()))));
        var props = world.ownerService.propertiesOf(owner.id());
        grand = props.stream().filter(p -> p.name().equals("Grand")).findFirst().orElseThrow();
        budget = props.stream().filter(p -> p.name().equals("Budget")).findFirst().orElseThrow();
    }

    private List<PropertySearchResult> search(String city, String locality, int guests, Map<String, String> filters) {
        return world.searchService.search(new SearchQuery(city, locality, world.stay(10, 2), guests, filters));
    }

    private List<String> names(List<PropertySearchResult> results) {
        return results.stream().map(r -> r.property().name()).toList();
    }

    @Test
    void findsPropertiesByCityIgnoringCaseSortedByPrice() {
        assertThat(names(search("BENGALURU", null, 2, Map.of()))).containsExactly("Budget", "Grand");
    }

    @Test
    void localityNarrowsTheCity() {
        assertThat(names(search("Bengaluru", "indiranagar", 2, Map.of()))).containsExactly("Grand");
    }

    @Test
    void guestCountHidesRoomsThatAreTooSmall() {
        var results = search("Bengaluru", "Indiranagar", 3, Map.of());
        assertThat(results).hasSize(1);
        assertThat(results.get(0).offers()).extracting(o -> o.roomType().name()).containsExactly("Family");
    }

    @Test
    void soldOutRoomsAreNotReturned() {
        world.bookingService.book(new com.rupeek.hotel.booking.BookingRequest(grand.id(), grand.roomTypes().get(0).id(),
                world.stay(10, 2), 2, new com.rupeek.hotel.booking.GuestDetails("G", "g@test.example")));

        var results = search("Bengaluru", "Indiranagar", 2, Map.of());
        assertThat(results.get(0).offers()).extracting(o -> o.roomType().name()).containsExactly("Family");

        // different dates: Deluxe is available again
        var later = world.searchService.search(new SearchQuery("Bengaluru", "Indiranagar", world.stay(20, 2), 2, Map.of()));
        assertThat(later.get(0).offers()).hasSize(2);
    }

    @Test
    void propertyDropsOutWhenEveryRoomIsTaken() {
        for (int i = 0; i < 5; i++) {
            world.bookingService.book(new com.rupeek.hotel.booking.BookingRequest(budget.id(), budget.roomTypes().get(0).id(),
                    world.stay(10, 2), 2, new com.rupeek.hotel.booking.GuestDetails("G", "g@test.example")));
        }
        assertThat(names(search("Bengaluru", null, 2, Map.of()))).containsExactly("Grand");
    }

    @Test
    void priceFilterUsesNightlyRate() {
        assertThat(names(search("Bengaluru", null, 2, Map.of("maxPrice", "5000")))).containsExactly("Budget");
        assertThat(names(search("Bengaluru", null, 2, Map.of("minPrice", "5000")))).containsExactly("Grand");
    }

    @Test
    void starAndAmenityFiltersCombine() {
        assertThat(names(search("Bengaluru", null, 2, Map.of("minStars", "4")))).containsExactly("Grand");
        assertThat(names(search("Bengaluru", null, 2, Map.of("amenities", "wifi,pool")))).containsExactly("Grand");
        // "breakfast" is a room-level amenity of Grand's Deluxe only
        var withBreakfast = search("Bengaluru", null, 2, Map.of("amenities", "wifi,breakfast"));
        assertThat(withBreakfast.get(0).offers()).extracting(o -> o.roomType().name()).containsExactly("Deluxe");
        assertThat(search("Bengaluru", null, 2, Map.of("minStars", "4", "maxPrice", "3000"))).isEmpty();
    }

    @Test
    void unknownFilterKeysAreIgnoredAndBadValuesRejected() {
        assertThat(search("Bengaluru", null, 2, Map.of("somethingElse", "x"))).hasSize(2);
        assertThatThrownBy(() -> search("Bengaluru", null, 2, Map.of("minStars", "lots")))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> search("Bengaluru", null, 2, Map.of("minPrice", "9", "maxPrice", "1")))
                .isInstanceOf(ValidationException.class);
    }
}
