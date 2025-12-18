# Azure Entra Group Name Mapper for Apache Guacamole

This extension enhances Apache Guacamole’s OIDC authentication by translating Azure Entra ID group object IDs (GUIDs) into human-readable group display names.

Guacamole normally receives only group GUIDs from Entra ID tokens, which makes group-based authorization hard to manage. This extension intercepts the authenticated user after login, resolves group GUIDs via Microsoft Graph, and exposes the corresponding group display names to Guacamole.

As a result, administrators can use Azure group names directly in Guacamole for permissions and access control instead of unreadable GUIDs.