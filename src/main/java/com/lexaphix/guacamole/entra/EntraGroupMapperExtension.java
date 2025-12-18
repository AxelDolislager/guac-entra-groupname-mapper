package com.lexaphix.guacamole.entra;

import org.apache.guacamole.net.auth.*;
import org.apache.guacamole.GuacamoleException;

public class EntraGroupMapperExtension implements AuthenticationProvider {

    private final WrappedAuthenticationProvider provider;

    public EntraGroupMapperExtension() throws GuacamoleException {
        String tenantId = System.getenv("GUAC_ENTRA_TENANT_ID");
        String clientId = System.getenv("GUAC_ENTRA_CLIENT_ID");
        String clientSecret = System.getenv("GUAC_ENTRA_CLIENT_SECRET");

        if (tenantId == null || clientId == null || clientSecret == null) {
            throw new GuacamoleException("Environment variables GUAC_ENTRA_* not set");
        }

        provider = new WrappedAuthenticationProvider(tenantId, clientId, clientSecret);
    }

    @Override
    public AuthenticatedUser updateAuthenticatedUser(AuthenticatedUser user, Credentials credentials) throws GuacamoleException {
        return provider.updateAuthenticatedUser(user, credentials);
    }

    @Override
    public UserContext updateUserContext(UserContext context, AuthenticatedUser user, Credentials credentials) throws GuacamoleException {
        return provider.updateUserContext(context, user, credentials);
    }

    @Override
    public void shutdown() {
        provider.shutdown();
    }

    @Override
    public String getIdentifier() {
        return "entra-group-mapper";
    }

    @Override
    public void decorate(UserContext existingContext, UserContext newContext, AuthenticatedUser user, Credentials credentials) throws GuacamoleException {
        provider.decorate(existingContext, newContext, user, credentials);
    }

    @Override
    public void redecorate(UserContext existingContext, UserContext newContext, AuthenticatedUser user, Credentials credentials) throws GuacamoleException {
        provider.redecorate(existingContext, newContext, user, credentials);
    }
}
