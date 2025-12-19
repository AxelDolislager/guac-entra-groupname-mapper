package org.apache.guacamole.auth.entragroups;

import com.google.inject.Inject;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.net.auth.AbstractAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import org.json.JSONObject;
import org.json.JSONArray;

/**
 * Authentication provider that enriches Entra ID SSO group claims
 * by converting ObjectIDs to group display names using Microsoft Graph API.
 */
public class EntraGroupNameProvider extends AbstractAuthenticationProvider {

    private static final Logger logger = LoggerFactory.getLogger(EntraGroupNameProvider.class);

    @Inject
    private Environment environment;

    /**
     * Cache for group ObjectID to DisplayName mappings
     * Key: ObjectID, Value: DisplayName
     */
    private final Map<String, String> groupCache = new HashMap<>();
    private long cacheExpiry = 0;
    private static final long CACHE_DURATION = 3600000; // 1 hour

    @Override
    public String getIdentifier() {
        return "entra-group-names";
    }

    @Override
    public AuthenticatedUser authenticateUser(Credentials credentials) throws GuacamoleException {
        // This extension doesn't authenticate - it decorates
        return null;
    }

    @Override
    public AuthenticatedUser updateAuthenticatedUser(
            AuthenticatedUser authenticatedUser,
            Credentials credentials) throws GuacamoleException {

        // Get the user's effective groups
        Set<String> effectiveGroups = authenticatedUser.getEffectiveUserGroups();
        
        if (effectiveGroups == null || effectiveGroups.isEmpty()) {
            return authenticatedUser;
        }

        try {
            // Get configuration
            String tenantId = environment.getProperty("entra-tenant-id");
            String clientId = environment.getProperty("entra-client-id");
            String clientSecret = environment.getProperty("entra-client-secret");
            
            if (tenantId == null || clientId == null || clientSecret == null) {
                logger.warn("Entra Group Name extension is not properly configured. " +
                           "Missing tenant-id, client-id, or client-secret.");
                return authenticatedUser;
            }

            // Refresh cache if expired
            if (System.currentTimeMillis() > cacheExpiry) {
                refreshGroupCache(tenantId, clientId, clientSecret);
            }

            // Create new set with resolved group names
            Set<String> resolvedGroups = new HashSet<>();
            
            for (String groupId : effectiveGroups) {
                // Check if this looks like an ObjectID (GUID format)
                if (isObjectId(groupId)) {
                    String displayName = groupCache.get(groupId);
                    if (displayName != null) {
                        resolvedGroups.add(displayName);
                        logger.debug("Resolved group {} to {}", groupId, displayName);
                    } else {
                        // Keep original if not found in cache
                        resolvedGroups.add(groupId);
                        logger.debug("Could not resolve group {}, keeping ObjectID", groupId);
                    }
                } else {
                    // Already a name, not an ObjectID
                    resolvedGroups.add(groupId);
                }
            }

            // Return decorated user with resolved group names
            return new DecoratedUser(authenticatedUser, resolvedGroups);
            
        } catch (Exception e) {
            logger.error("Error resolving Entra group names", e);
            return authenticatedUser;
        }
    }

    /**
     * Checks if a string matches GUID/ObjectID format
     */
    private boolean isObjectId(String id) {
        return id != null && id.matches(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
        );
    }

    /**
     * Refreshes the group cache by querying Microsoft Graph API
     */
    private void refreshGroupCache(String tenantId, String clientId, String clientSecret) {
        try {
            // Get access token
            String accessToken = getAccessToken(tenantId, clientId, clientSecret);
            
            // Query all groups
            String groupsUrl = "https://graph.microsoft.com/v1.0/groups?$select=id,displayName";
            
            groupCache.clear();
            fetchAllGroups(groupsUrl, accessToken);
            
            cacheExpiry = System.currentTimeMillis() + CACHE_DURATION;
            logger.info("Refreshed group cache with {} entries", groupCache.size());
            
        } catch (Exception e) {
            logger.error("Failed to refresh group cache", e);
        }
    }

    /**
     * Fetches all groups with pagination support
     */
    private void fetchAllGroups(String url, String accessToken) throws Exception {
        HttpURLConnection conn = null;
        
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setRequestProperty("Content-Type", "application/json");
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                logger.error("Failed to fetch groups: HTTP {}", responseCode);
                return;
            }
            
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            
            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONArray groups = jsonResponse.getJSONArray("value");
            
            for (int i = 0; i < groups.length(); i++) {
                JSONObject group = groups.getJSONObject(i);
                String id = group.getString("id");
                String displayName = group.getString("displayName");
                groupCache.put(id, displayName);
            }
            
            // Handle pagination
            if (jsonResponse.has("@odata.nextLink")) {
                String nextLink = jsonResponse.getString("@odata.nextLink");
                fetchAllGroups(nextLink, accessToken);
            }
            
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Gets an access token from Microsoft Entra ID
     */
    private String getAccessToken(String tenantId, String clientId, String clientSecret) 
            throws Exception {
        
        String tokenUrl = "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token";
        
        String postData = "grant_type=client_credentials" +
                         "&client_id=" + clientId +
                         "&client_secret=" + clientSecret +
                         "&scope=https://graph.microsoft.com/.default";
        
        HttpsURLConnection conn = null;
        
        try {
            conn = (HttpsURLConnection) new URL(tokenUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);
            
            conn.getOutputStream().write(postData.getBytes("UTF-8"));
            
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new Exception("Failed to get access token: HTTP " + responseCode);
            }
            
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();
            
            JSONObject jsonResponse = new JSONObject(response.toString());
            return jsonResponse.getString("access_token");
            
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Decorated AuthenticatedUser that overrides group information
     */
    private static class DecoratedUser implements AuthenticatedUser {
        
        private final AuthenticatedUser wrapped;
        private final Set<String> effectiveGroups;
        
        public DecoratedUser(AuthenticatedUser wrapped, Set<String> effectiveGroups) {
            this.wrapped = wrapped;
            this.effectiveGroups = Collections.unmodifiableSet(effectiveGroups);
        }
        
        @Override
        public String getIdentifier() {
            return wrapped.getIdentifier();
        }
        
        @Override
        public void setIdentifier(String identifier) {
            wrapped.setIdentifier(identifier);
        }
        
        @Override
        public Credentials getCredentials() {
            return wrapped.getCredentials();
        }
        
        @Override
        public Set<String> getEffectiveUserGroups() {
            return effectiveGroups;
        }
        
        @Override
        public AuthenticationProvider getAuthenticationProvider() {
            return wrapped.getAuthenticationProvider();
        }
        
        @Override
        public void invalidate() {
            wrapped.invalidate();
        }
        
        @Override
        public UserContext getUserContext() throws GuacamoleException {
            return wrapped.getUserContext();
        }
    }
}