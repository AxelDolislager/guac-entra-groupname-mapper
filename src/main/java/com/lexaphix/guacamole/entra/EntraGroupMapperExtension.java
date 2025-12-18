public class EntraGroupMapperExtension extends AuthenticationProvider {

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        AuthenticationProvider base = AuthenticationProviderServices.getInstance().getProvider("openid");
        return new WrappedAuthenticationProvider(base);
    }
}
