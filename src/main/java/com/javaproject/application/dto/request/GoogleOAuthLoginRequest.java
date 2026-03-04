package com.javaproject.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class GoogleOAuthLoginRequest extends BaseRequest {

    @NotBlank
    @Pattern(regexp = "GOOGLE", message = "Only GOOGLE provider is currently supported.")
    private String provider;

    @NotBlank
    @Size(max = 4096)
    private String authorizationCode;

    @NotBlank
    @Size(max = 1024)
    private String redirectUri;

    @Size(max = 512)
    private String codeVerifier;

    @Size(max = 255)
    private String nonce;
}
