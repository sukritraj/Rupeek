package com.rupeek.hotel;

import com.rupeek.hotel.common.Money;
import com.rupeek.hotel.common.exception.NotFoundException;
import com.rupeek.hotel.common.exception.ValidationException;
import com.rupeek.hotel.property.Owner;
import com.rupeek.hotel.property.PropertyDraft.RoomTypeDraft;
import com.rupeek.hotel.support.TestWorld;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OwnerServiceTest {

    private final TestWorld world = new TestWorld();
    private final RoomTypeDraft room = new RoomTypeDraft("Standard", 2, 3, Money.of(3000), Set.of());

    @Test
    void standaloneHotelIsJustAnOwnerWithOneProperty() {
        Owner owner = world.ownerService.register("Asha", "asha@test.example",
                List.of(TestWorld.draft("Asha Stay", "Goa", "Baga", 3, Set.of(), room)));

        assertThat(owner.propertyIds()).hasSize(1);
    }

    @Test
    void chainOwnsManyPropertiesAndCanGrow() {
        Owner owner = world.ownerService.register("Chain", "chain@test.example", List.of(
                TestWorld.draft("A", "Goa", "Baga", 3, Set.of(), room),
                TestWorld.draft("B", "Goa", "Anjuna", 4, Set.of(), room)));
        world.ownerService.addProperty(owner.id(), TestWorld.draft("C", "Pune", null, 3, Set.of(), room));

        assertThat(world.ownerService.propertiesOf(owner.id())).extracting("name").containsExactlyInAnyOrder("A", "B", "C");
        assertThat(world.ownerService.getOwner(owner.id()).propertyIds()).hasSize(3);
    }

    @Test
    void invalidDraftSavesNothing() {
        var good = TestWorld.draft("Good", "Goa", "Baga", 3, Set.of(), room);
        var bad = TestWorld.draft("Bad", "Goa", "Baga", 9, Set.of(), room);

        assertThatThrownBy(() -> world.ownerService.register("Chain", "chain@test.example", List.of(good, bad)))
                .isInstanceOf(ValidationException.class);
        assertThat(world.properties.findByLocation("Goa", null)).isEmpty();
    }

    @Test
    void rejectsDuplicateRoomTypesAndEmptyOnboarding() {
        var dup = TestWorld.draft("Dup", "Goa", "Baga", 3, Set.of(), room, room);
        assertThatThrownBy(() -> world.ownerService.register("X", "x@test.example", List.of(dup)))
                .isInstanceOf(ValidationException.class).hasMessageContaining("Duplicate");
        assertThatThrownBy(() -> world.ownerService.register("X", "x@test.example", List.of()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void addingToUnknownOwnerFails() {
        assertThatThrownBy(() -> world.ownerService.addProperty("owner-missing",
                TestWorld.draft("A", "Goa", "Baga", 3, Set.of(), room)))
                .isInstanceOf(NotFoundException.class);
    }
}
