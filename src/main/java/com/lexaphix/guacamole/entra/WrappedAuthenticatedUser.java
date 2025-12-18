package com.lexaphix.guacamole.entra;

import org.apache.guacamole.net.auth.*;
import java.util.*;

public class WrappedAuthenticatedUser implements AuthenticatedUser {

    private final AuthenticatedUser baseUser;
    private final EntraGroupMappingService groupService;
    private final AuthenticationProvider provider;

    public WrappedAuthenticatedUser(AuthenticatedUser baseUser, EntraGroupMappingService groupService, AuthenticationProvider provider) {
        this.baseUser = baseUser;
        this.groupService = groupService;
        this.provider = provider;
    }

    @Override
    public Credentials getCredentials() {
        return baseUser.getCredentials();
    }

    @Override
    public Set<String> getEffectiveUserGroups() {
        Set<String> originalGroups = baseUser.getEffectiveUserGroups();
        Set<String> mappedGroups = new HashSet<>();
        for (String groupId : originalGroups) {
            mappedGroups.add(groupService.getGroupName(groupId));
        }
        return mappedGroups;
    }

    @Override
    public void invalidate() {
        baseUser.invalidate();
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return provider;
    }
}
