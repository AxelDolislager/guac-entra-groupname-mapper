package com.lexaphix.guacamole.entra;

import java.io.*;
import java.net.*;
import java.util.*;

public class GraphClient {

    private final String tenantId;
    private final String clientId;
    private final String clientSecret;

    public GraphClient(String tenantId, String clientId, String clientSecret) {
        this.tenantId = tenantId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public String getGroupName(String groupId) throws IOException {
        // Dummy implementatie; hier zou je Microsoft Graph API call doen
        // Gebruik tenantId/clientId/clientSecret voor authenticatie
        return "Group-" + groupId;
    }
}
