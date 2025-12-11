package com.web.backend.controller.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AddressResponse {
    private Long id;
    private String houseNumber;
    private String street;
    private String ward;
    private String district;
    private String city;
    private String country;
}