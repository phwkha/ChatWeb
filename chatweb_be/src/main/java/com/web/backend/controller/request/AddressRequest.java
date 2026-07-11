package com.web.backend.controller.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddressRequest {
    private String houseNumber;

    @NotBlank(message = "{valid.street_empty}")
    private String street;

    @NotBlank(message = "{valid.ward_empty}")
    private String ward;

    @NotBlank(message = "{valid.district_empty}")
    private String district;

    @NotBlank(message = "{valid.city_empty}")
    private String city;

    @NotBlank(message = "{valid.country_empty}")
    private String country;

    private String postalCode;
}