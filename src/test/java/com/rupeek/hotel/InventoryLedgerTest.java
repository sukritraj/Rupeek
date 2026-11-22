package com.rupeek.hotel;

import com.rupeek.hotel.common.DateRange;
import com.rupeek.hotel.inventory.InMemoryInventoryLedger;
import com.rupeek.hotel.inventory.InventoryLedger;
import com.rupeek.hotel.property.RoomType;
import com.rupeek.hotel.common.Money;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryLedgerTest {

    private final InventoryLedger ledger = new InMemoryInventoryLedger();
    private final RoomType twoRooms = new RoomType("Standard", 2, 2, Money.of(1000), Set.of());
    private final LocalDate day = LocalDate.of(2026, 8, 10);

    private DateRange nights(int from, int count) {
        return new DateRange(day.plusDays(from), day.plusDays(from + count));
    }

    @Test
    void cannotSellMoreRoomsThanExist() {
        assertThat(ledger.reserve(twoRooms, nights(0, 2))).isTrue();
        assertThat(ledger.reserve(twoRooms, nights(0, 2))).isTrue();
        assertThat(ledger.reserve(twoRooms, nights(0, 2))).isFalse();
        assertThat(ledger.availableRooms(twoRooms, nights(0, 2))).isZero();
    }

    @Test
    void oneSoldOutNightBlocksTheWholeStay() {
        ledger.reserve(twoRooms, nights(1, 1));
        ledger.reserve(twoRooms, nights(1, 1));

        // night 0..3 includes the sold-out night 1
        assertThat(ledger.reserve(twoRooms, nights(0, 3))).isFalse();
        // and nothing was partially held for the nights that were free
        assertThat(ledger.availableRooms(twoRooms, nights(0, 1))).isEqualTo(2);
        assertThat(ledger.availableRooms(twoRooms, nights(2, 1))).isEqualTo(2);
    }

    @Test
    void backToBackStaysDoNotCollide() {
        assertThat(ledger.reserve(twoRooms, nights(0, 2))).isTrue();
        assertThat(ledger.reserve(twoRooms, nights(0, 2))).isTrue();
        assertThat(ledger.reserve(twoRooms, nights(2, 2))).isTrue();
    }

    @Test
    void releaseMakesRoomBookableAgain() {
        ledger.reserve(twoRooms, nights(0, 2));
        ledger.reserve(twoRooms, nights(0, 2));
        ledger.release(twoRooms.id(), nights(0, 2));

        assertThat(ledger.availableRooms(twoRooms, nights(0, 2))).isEqualTo(1);
        assertThat(ledger.reserve(twoRooms, nights(0, 2))).isTrue();
    }

    @Test
    void releasingMoreThanHeldNeverGoesNegative() {
        ledger.release(twoRooms.id(), nights(0, 2));
        assertThat(ledger.availableRooms(twoRooms, nights(0, 2))).isEqualTo(2);
    }

    @Test
    void concurrentGuestsNeverOversellTheLastRooms() throws Exception {
        RoomType fiveRooms = new RoomType("Suite", 2, 5, Money.of(1000), Set.of());
        int guests = 40;
        ExecutorService pool = Executors.newFixedThreadPool(guests);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < guests; i++) {
            Callable<Boolean> attempt = () -> {
                start.await();
                return ledger.reserve(fiveRooms, nights(0, 3));
            };
            results.add(pool.submit(attempt));
        }
        start.countDown();

        long won = 0;
        for (Future<Boolean> f : results) {
            if (f.get()) {
                won++;
            }
        }
        pool.shutdown();

        assertThat(won).isEqualTo(5);
        assertThat(ledger.availableRooms(fiveRooms, nights(0, 3))).isZero();
    }
}
