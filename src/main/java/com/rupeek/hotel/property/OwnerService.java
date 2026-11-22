package com.rupeek.hotel.property;

import com.rupeek.hotel.common.exception.NotFoundException;
import com.rupeek.hotel.common.exception.ValidationException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Onboarding entry point. Registering a chain and registering a single hotel go through
 * the same method - the latter just passes a one element list.
 */
@Service
public class OwnerService {

    private final OwnerRepository owners;
    private final PropertyRepository properties;

    public OwnerService(OwnerRepository owners, PropertyRepository properties) {
        this.owners = owners;
        this.properties = properties;
    }

    public Owner register(String name, String email, List<PropertyDraft> drafts) {
        if (drafts == null || drafts.isEmpty()) {
            throw new ValidationException("An owner must onboard at least one property");
        }
        Owner owner = new Owner(name, email);
        // build everything first so a bad draft leaves nothing half-saved
        List<Property> built = drafts.stream().map(d -> build(owner.id(), d)).toList();
        owners.save(owner);
        built.forEach(p -> attach(owner, p));
        return owner;
    }

    public Property addProperty(String ownerId, PropertyDraft draft) {
        Owner owner = getOwner(ownerId);
        Property property = build(ownerId, draft);
        attach(owner, property);
        return property;
    }

    public Owner getOwner(String ownerId) {
        return owners.findById(ownerId).orElseThrow(() -> new NotFoundException("Owner", ownerId));
    }

    public Property getProperty(String propertyId) {
        return properties.findById(propertyId).orElseThrow(() -> new NotFoundException("Property", propertyId));
    }

    public List<Property> propertiesOf(String ownerId) {
        getOwner(ownerId);
        return properties.findByOwner(ownerId);
    }

    private void attach(Owner owner, Property property) {
        properties.save(property);
        owner.addProperty(property.id());
    }

    private Property build(String ownerId, PropertyDraft draft) {
        if (draft.roomTypes() == null) {
            throw new ValidationException("Property '" + draft.name() + "' needs at least one room type");
        }
        List<RoomType> rooms = draft.roomTypes().stream()
                .map(r -> new RoomType(r.name(), r.maxGuests(), r.totalRooms(), r.basePrice(), r.amenities()))
                .toList();
        return new Property(ownerId, draft.name(), new Address(draft.city(), draft.locality()),
                draft.starRating(), draft.amenities(), rooms);
    }
}
