package com.rupeek.hotel.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** A standalone hotel is just a request with a single entry in {@code properties}. */
public record RegisterOwnerRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotEmpty List<@Valid PropertyRequest> properties) {
}
