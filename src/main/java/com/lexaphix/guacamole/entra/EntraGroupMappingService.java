package com.lexaphix.guacamole.entra;

import java.util.HashMap;
import java.util.Map;

public class EntraGroupMappingService {

    private final GraphClient graphClient;
    private final Map<String, String> cache = new HashMap<>();

    public EntraGroupMappingService(String tenantId, String clientId, String clientSecret) throws Exception {
        this.graphClient = new GraphClient(tenantId, clientId, clientSecret);
    }

    public String getGroupName(String groupId) {
        if (cache.containsKey(groupId)) {
            return cache.get(groupId);
        }
        try {
            String name = graphClient.getGroupName(groupId);
            cache.put(groupId, name);
            return name;
        } catch (Exception e) {
            e.printStackTrace();
            return groupId;
        }
    }
}
