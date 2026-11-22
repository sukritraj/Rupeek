package com.rupeek.hotel.config;

import com.rupeek.hotel.common.Money;
import com.rupeek.hotel.property.OwnerService;
import com.rupeek.hotel.property.PropertyDraft;
import com.rupeek.hotel.property.PropertyDraft.RoomTypeDraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/** Seeds one chain and one standalone hotel so the API can be tried straight after startup. */
@Component
@ConditionalOnProperty(name = "hotel.seed.enabled", havingValue = "true")
public class DemoDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private final OwnerService ownerService;

    public DemoDataLoader(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    @Override
    public void run(ApplicationArguments args) {
        var chain = ownerService.register("Lakeview Hotels", "ops@lakeview.example", List.of(
                new PropertyDraft("Lakeview Grand", "Bengaluru", "Indiranagar", 5, Set.of("wifi", "pool", "spa", "parking"),
                        List.of(new RoomTypeDraft("Deluxe King", 2, 5, Money.of(8500), Set.of("breakfast")),
                                new RoomTypeDraft("Family Suite", 4, 2, Money.of(14000), Set.of("breakfast", "kitchenette")))),
                new PropertyDraft("Lakeview Express", "Bengaluru", "Whitefield", 3, Set.of("wifi", "parking"),
                        List.of(new RoomTypeDraft("Standard", 2, 10, Money.of(3200), Set.of()))),
                new PropertyDraft("Lakeview Bay", "Mumbai", "Bandra", 4, Set.of("wifi", "gym"),
                        List.of(new RoomTypeDraft("Sea View", 3, 4, Money.of(11000), Set.of("breakfast"))))));

        var standalone = ownerService.register("Asha Guest House", "asha@example.com", List.of(
                new PropertyDraft("Asha Homestay", "Bengaluru", "Indiranagar", 2, Set.of("wifi"),
                        List.of(new RoomTypeDraft("Double", 2, 3, Money.of(2100), Set.of())))));

        log.info("Seeded demo owners: {} (chain) and {} (standalone)", chain.id(), standalone.id());
    }
}
