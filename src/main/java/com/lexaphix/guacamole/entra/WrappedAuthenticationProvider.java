public class WrappedAuthenticationProvider implements AuthenticationProvider {

    private final AuthenticationProvider delegate;
    private final EntraGroupMappingService mappingService = new EntraGroupMappingService();

    public WrappedAuthenticationProvider(AuthenticationProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public AuthenticatedUser authenticateUser(Credentials credentials)
            throws GuacamoleException {

        AuthenticatedUser user = delegate.authenticateUser(credentials);

        if (user == null)
            return null;

        Set<String> mapped = mappingService.mapGuidsToNames(user.getEffectiveGroups());

        return new WrappedAuthenticatedUser(user, mapped);
    }
}
