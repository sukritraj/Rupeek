package com.rupeek.hotel.property;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** Amenities are compared case-insensitively, so we store them lower-cased. */
public final class Amenities {

    private Amenities() {
    }

    public static Set<String> normalise(Collection<String> raw) {
        Set<String> out = new LinkedHashSet<>();
        if (raw != null) {
            for (String a : raw) {
                if (a != null && !a.isBlank()) {
                    out.add(a.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        return Collections.unmodifiableSet(out);
    }
}
