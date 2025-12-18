package com.lexaphix.guacamole.entra;

import org.apache.guacamole.net.auth.*;
import org.apache.guacamole.GuacamoleException;

public class WrappedAuthenticationProvider implements AuthenticationProvider {

    private final EntraGroupMappingService groupService;

    public WrappedAuthenticationProvider(String tenantId, String clientId, String clientSecret) throws GuacamoleException {
        try {
            groupService = new EntraGroupMappingService(tenantId, clientId, clientSecret);
        } catch (Exception e) {
            throw new GuacamoleException("Failed to initialize EntraGroupMappingService", e);
        }
    }

    @Override
    public AuthenticatedUser updateAuthenticatedUser(AuthenticatedUser user, Credentials credentials) throws GuacamoleException {
        return new WrappedAuthenticatedUser(user, groupService, this);
    }

    @Override
    public UserContext updateUserContext(UserContext context, AuthenticatedUser user, Credentials credentials) throws GuacamoleException {
        return context;
    }

    @Override
    public void shutdown() {
        // cleanup indien nodig
    }

    @Override
    public String getIdentifier() {
        return "entra-group-mapper";
    }

    @Override
    public void decorate(UserContext existingContext, UserContext newContext, AuthenticatedUser user, Credentials credentials) throws GuacamoleException {
        // geen logica voor nu
    }

    @Override
    public void redecorate(UserContext existingContext, UserContext newContext, AuthenticatedUser user, Credentials credentials) throws GuacamoleException {
        // geen logica voor nu
    }
}
