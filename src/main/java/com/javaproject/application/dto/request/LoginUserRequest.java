package com.javaproject.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginUserRequest extends BaseRequest {

    @Email
    @Size(max = 255)
    private String email;

    @Size(min = 8, max = 255)
    private String password;

    @Pattern(
            regexp = "^\\+?[1-9]\\d{7,14}$",
            message = "Mobile number must be in valid international format."
    )
    private String mobile;

    @Pattern(regexp = "\\d{6}", message = "OTP must be exactly 6 digits")
    private String otp;

    @Size(max = 128)
    private String deviceName;

    @Size(max = 255)
    private String deviceFingerprint;

    @Size(max = 32)
    private String authenticationMethod;
}
