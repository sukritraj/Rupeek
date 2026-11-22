package com.rupeek.hotel.property;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryPropertyRepository implements PropertyRepository {

    private final Map<String, Property> store = new ConcurrentHashMap<>();

    @Override
    public Property save(Property property) {
        store.put(property.id(), property);
        return property;
    }

    @Override
    public Optional<Property> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Property> findByOwner(String ownerId) {
        return store.values().stream().filter(p -> p.ownerId().equals(ownerId)).toList();
    }

    @Override
    public List<Property> findByLocation(String city, String locality) {
        return store.values().stream().filter(p -> p.address().isIn(city, locality)).toList();
    }
}
