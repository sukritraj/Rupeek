package com.rupeek.hotel.api;

import com.rupeek.hotel.api.dto.OwnerResponse;
import com.rupeek.hotel.api.dto.PropertyRequest;
import com.rupeek.hotel.api.dto.PropertyResponse;
import com.rupeek.hotel.api.dto.RegisterOwnerRequest;
import com.rupeek.hotel.property.Owner;
import com.rupeek.hotel.property.OwnerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class OwnerController {

    private final OwnerService ownerService;

    public OwnerController(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    @PostMapping("/owners")
    @ResponseStatus(HttpStatus.CREATED)
    public OwnerResponse register(@Valid @RequestBody RegisterOwnerRequest request) {
        Owner owner = ownerService.register(request.name(), request.email(),
                request.properties().stream().map(PropertyRequest::toDraft).toList());
        return OwnerResponse.from(owner, ownerService.propertiesOf(owner.id()));
    }

    @GetMapping("/owners/{ownerId}")
    public OwnerResponse owner(@PathVariable String ownerId) {
        Owner owner = ownerService.getOwner(ownerId);
        return OwnerResponse.from(owner, ownerService.propertiesOf(ownerId));
    }

    @PostMapping("/owners/{ownerId}/properties")
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyResponse addProperty(@PathVariable String ownerId, @Valid @RequestBody PropertyRequest request) {
        return PropertyResponse.from(ownerService.addProperty(ownerId, request.toDraft()));
    }

    @GetMapping("/properties/{propertyId}")
    public PropertyResponse property(@PathVariable String propertyId) {
        return PropertyResponse.from(ownerService.getProperty(propertyId));
    }
}
