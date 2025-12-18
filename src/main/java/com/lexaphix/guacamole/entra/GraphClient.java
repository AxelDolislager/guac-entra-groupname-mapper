public class GraphClient {

    private final GraphServiceClient graphClient;

    public GraphClient() {
        TokenCredential credential = new ClientSecretCredentialBuilder()
                .clientId(System.getenv("ENTRA_CLIENT_ID"))
                .clientSecret(System.getenv("ENTRA_CLIENT_SECRET"))
                .tenantId(System.getenv("ENTRA_TENANT_ID"))
                .build();

        graphClient = new GraphServiceClient(credential, List.of("https://graph.microsoft.com/.default"));
    }

    public String getGroupName(String guid) {
        try {
            var group = graphClient.groups(guid).buildRequest().get();
            return group.displayName;
        } catch (Exception e) {
            return null;
        }
    }
}
