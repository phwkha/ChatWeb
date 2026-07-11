package com.web.backend.controller.request;

import com.web.backend.common.GenderType;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.Date;

@Data
public class UpdateUserRequest {
    private String firstName;
    private String lastName;

    @Pattern(
            regexp = "^(0[0-9]{9}|\\+84[0-9]{9})$",
            message = "{valid.phone_invalid}"
    )
    private String phone;
    private Date birthday;
    private GenderType gender;
}