package com.rupeek.hotel.property;

import com.rupeek.hotel.common.Ids;
import com.rupeek.hotel.common.exception.ValidationException;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The account that owns properties. A chain has many; a standalone hotel is simply an
 * owner with exactly one - there is no separate "single property" code path.
 */
public final class Owner {

    private final String id;
    private final String name;
    private final String email;
    private final List<String> propertyIds = new CopyOnWriteArrayList<>();

    public Owner(String name, String email) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Owner name is required");
        }
        if (email == null || !email.contains("@")) {
            throw new ValidationException("A valid owner email is required");
        }
        this.id = Ids.next("owner");
        this.name = name.trim();
        this.email = email.trim();
    }

    public void addProperty(String propertyId) {
        propertyIds.add(propertyId);
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String email() {
        return email;
    }

    public List<String> propertyIds() {
        return List.copyOf(propertyIds);
    }
}
