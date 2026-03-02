package com.javaproject.application.security.authentication;

import com.javaproject.application.dto.request.BaseRequest;

/**
 * Strategy interface for authentication methods.
 * <p>
 * Each authentication mechanism (JWT/password, OAuth, SAML, API-key, etc.)
 * implements this interface. The {@link AuthenticationStrategyFactory} resolves
 * the correct strategy at runtime based on the request.
 */
public interface AuthenticationStrategy {

    /**
     * @return the identifier for this strategy (e.g. "JWT", "OAUTH2", "SAML")
     */
    String getStrategyName();

    /**
     * Authenticate the user based on the incoming request.
     *
     * @param request the incoming API request containing credentials
     * @return an {@link AuthenticationResult} with tokens and user context
     */
    AuthenticationResult authenticate(BaseRequest request);
}
