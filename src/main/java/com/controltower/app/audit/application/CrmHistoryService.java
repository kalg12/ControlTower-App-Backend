package com.controltower.app.audit.application;

import com.controltower.app.audit.domain.AuditAction;
import com.controltower.app.audit.domain.AuditLog;
import com.controltower.app.clients.domain.Client;
import com.controltower.app.clients.domain.ClientBranch;
import com.controltower.app.clients.domain.ClientContact;
import com.controltower.app.clients.domain.ClientOpportunity;
import com.controltower.app.notifications.application.NotificationService;
import com.controltower.app.notifications.domain.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrmHistoryService {

    private final AuditService auditService;
    private final NotificationService notificationService;

    public void logClientChange(UUID tenantId, UUID userId, Client oldClient, Client newClient, 
                                AuditAction action, UUID assignedUserId) {
        Map<String, Object> changes = detectClientChanges(oldClient, newClient);
        
        String oldValue = toJson(serializeClient(oldClient));
        String newValue = toJson(serializeClient(newClient));

        AuditLog entry = AuditLog.builder(action)
                .tenantId(tenantId)
                .userId(userId)
                .resource("CLIENT", newClient.getId().toString())
                .oldValue(oldValue)
                .newValue(newValue)
                .build();

        auditService.log(entry);
        notifyClientChange(tenantId, assignedUserId, action, changes, newClient.getName());
    }

    public void logBranchChange(UUID tenantId, UUID userId, ClientBranch oldBranch, ClientBranch newBranch,
                                 AuditAction action, UUID assignedUserId) {
        Map<String, Object> changes = detectBranchChanges(oldBranch, newBranch);
        
        String oldValue = toJson(serializeBranch(oldBranch));
        String newValue = toJson(serializeBranch(newBranch));

        AuditLog entry = AuditLog.builder(action)
                .tenantId(tenantId)
                .userId(userId)
                .resource("BRANCH", newBranch.getId().toString())
                .oldValue(oldValue)
                .newValue(newValue)
                .build();

        auditService.log(entry);
        notifyBranchChange(tenantId, assignedUserId, action, changes, newBranch.getName());
    }

    public void logOpportunityChange(UUID tenantId, UUID userId, ClientOpportunity oldOpp, 
                                      ClientOpportunity newOpp, AuditAction action, UUID assignedUserId) {
        Map<String, Object> changes = detectOpportunityChanges(oldOpp, newOpp);
        
        String oldValue = toJson(serializeOpportunity(oldOpp));
        String newValue = toJson(serializeOpportunity(newOpp));

        AuditLog entry = AuditLog.builder(action)
                .tenantId(tenantId)
                .userId(userId)
                .resource("OPPORTUNITY", newOpp.getId().toString())
                .oldValue(oldValue)
                .newValue(newValue)
                .build();

        auditService.log(entry);
        notifyOpportunityChange(tenantId, assignedUserId, action, changes, newOpp.getTitle());
    }

    public void logContactChange(UUID tenantId, UUID userId, ClientContact oldContact,
                                  ClientContact newContact, AuditAction action, UUID assignedUserId) {
        Map<String, Object> changes = detectContactChanges(oldContact, newContact);
        
        String oldValue = toJson(serializeContact(oldContact));
        String newValue = toJson(serializeContact(newContact));

        AuditLog entry = AuditLog.builder(action)
                .tenantId(tenantId)
                .userId(userId)
                .resource("CONTACT", newContact.getId().toString())
                .oldValue(oldValue)
                .newValue(newValue)
                .build();

        auditService.log(entry);
        notifyContactChange(tenantId, assignedUserId, action, changes, newContact.getFullName());
    }

    private Map<String, Object> detectClientChanges(Client oldClient, Client newClient) {
        Map<String, Object> changes = new LinkedHashMap<>();
        
        if (oldClient == null || !Objects.equals(oldClient.getStatus(), newClient.getStatus())) {
            changes.put("status", changeMap(
                oldClient != null ? oldClient.getStatus() : null,
                newClient.getStatus()
            ));
        }
        
        if (oldClient == null || !Objects.equals(oldClient.getAccountOwnerId(), newClient.getAccountOwnerId())) {
            changes.put("accountOwnerId", changeMap(
                oldClient != null ? oldClient.getAccountOwnerId() : null,
                newClient.getAccountOwnerId()
            ));
        }
        
        if (oldClient == null || !Objects.equals(oldClient.getSegment(), newClient.getSegment())) {
            changes.put("segment", changeMap(
                oldClient != null ? oldClient.getSegment() : null,
                newClient.getSegment()
            ));
        }

        return changes;
    }

    private Map<String, Object> detectBranchChanges(ClientBranch oldBranch, ClientBranch newBranch) {
        Map<String, Object> changes = new LinkedHashMap<>();
        
        if (oldBranch == null || !Objects.equals(oldBranch.getStatus(), newBranch.getStatus())) {
            changes.put("status", changeMap(
                oldBranch != null ? oldBranch.getStatus() : null,
                newBranch.getStatus()
            ));
        }

        return changes;
    }

    private Map<String, Object> detectOpportunityChanges(ClientOpportunity oldOpp, ClientOpportunity newOpp) {
        Map<String, Object> changes = new LinkedHashMap<>();
        
        if (oldOpp == null || !Objects.equals(oldOpp.getStage(), newOpp.getStage())) {
            changes.put("stage", changeMap(
                oldOpp != null ? oldOpp.getStage() : null,
                newOpp.getStage()
            ));
        }
        
        if (oldOpp == null || !Objects.equals(oldOpp.getValue(), newOpp.getValue())) {
            changes.put("value", changeMap(
                oldOpp != null ? oldOpp.getValue() : null,
                newOpp.getValue()
            ));
        }
        
        if (oldOpp == null || !Objects.equals(oldOpp.getOwnerId(), newOpp.getOwnerId())) {
            changes.put("ownerId", changeMap(
                oldOpp != null ? oldOpp.getOwnerId() : null,
                newOpp.getOwnerId()
            ));
        }

        return changes;
    }

    private Map<String, Object> detectContactChanges(ClientContact oldContact, ClientContact newContact) {
        Map<String, Object> changes = new LinkedHashMap<>();
        return changes;
    }

    private Map<String, Object> changeMap(Object oldValue, Object newValue) {
        Map<String, Object> change = new LinkedHashMap<>();
        change.put("old", oldValue);
        change.put("new", newValue);
        return change;
    }

    private void notifyClientChange(UUID tenantId, UUID userId, AuditAction action, 
                                    Map<String, Object> changes, String clientName) {
        if (userId == null) return;

        String title = getNotificationTitle(action);
        String body = getNotificationBody(action, changes, clientName);
        String type = getNotificationType(action);

        notificationService.send(tenantId, type, title, body, Notification.Severity.INFO, 
                Map.of("clientName", clientName != null ? clientName : "", "changes", changes),
                List.of(userId));
    }

    private void notifyBranchChange(UUID tenantId, UUID userId, AuditAction action,
                                    Map<String, Object> changes, String branchName) {
        if (userId == null) return;

        String title = getNotificationTitle(action);
        String body = getNotificationBody(action, changes, branchName);
        String type = getNotificationType(action);

        notificationService.send(tenantId, type, title, body, Notification.Severity.INFO,
                Map.of("branchName", branchName != null ? branchName : "", "changes", changes),
                List.of(userId));
    }

    private void notifyOpportunityChange(UUID tenantId, UUID userId, AuditAction action,
                                         Map<String, Object> changes, String oppTitle) {
        if (userId == null) return;

        String title = getNotificationTitle(action);
        String body = getNotificationBody(action, changes, oppTitle);
        String type = getNotificationType(action);

        notificationService.send(tenantId, type, title, body, Notification.Severity.INFO,
                Map.of("opportunityTitle", oppTitle != null ? oppTitle : "", "changes", changes),
                List.of(userId));
    }

    private void notifyContactChange(UUID tenantId, UUID userId, AuditAction action,
                                      Map<String, Object> changes, String contactName) {
        if (userId == null) return;

        String title = getNotificationTitle(action);
        String body = getNotificationBody(action, changes, contactName);
        String type = getNotificationType(action);

        notificationService.send(tenantId, type, title, body, Notification.Severity.INFO,
                Map.of("contactName", contactName != null ? contactName : "", "changes", changes),
                List.of(userId));
    }

    private String getNotificationTitle(AuditAction action) {
        return switch (action) {
            case CLIENT_STATUS_CHANGED -> "Cliente movido";
            case CLIENT_ASSIGNED_USER_CHANGED -> "Cliente reasignado";
            case CLIENT_SEGMENT_CHANGED -> "Segmento de cliente cambiado";
            case PROSPECT_CONVERTED -> "Prospecto convertido a cliente";
            case PROSPECT_LOST -> "Prospecto perdido";
            case PROSPECT_REACTIVATED -> "Prospecto reactivado";
            case BRANCH_STATUS_CHANGED -> "Sucursal movida";
            case BRANCH_ASSIGNED_USER_CHANGED -> "Sucursal reasignada";
            case OPPORTUNITY_STAGE_CHANGED -> "Etapa de oportunidad cambiada";
            case OPPORTUNITY_VALUE_CHANGED -> "Valor de oportunidad actualizado";
            case OPPORTUNITY_ASSIGNED_USER_CHANGED -> "Oportunidad reasignada";
            case OPPORTUNITY_WON -> "Oportunidad ganada";
            case OPPORTUNITY_LOST -> "Oportunidad perdida";
            case OPPORTUNITY_REOPENED -> "Oportunidad reopenida";
            case CONTACT_ASSIGNED_USER_CHANGED -> "Contacto reasignado";
            default -> "Cambio en CRM";
        };
    }

    private String getNotificationBody(AuditAction action, Map<String, Object> changes, String entityName) {
        StringBuilder sb = new StringBuilder();
        
        if (changes.containsKey("status")) {
            var statusChange = (Map<String, Object>) changes.get("status");
            sb.append("Estado: ").append(statusChange.get("old")).append(" → ").append(statusChange.get("new"));
        } else if (changes.containsKey("stage")) {
            var stageChange = (Map<String, Object>) changes.get("stage");
            sb.append("Etapa: ").append(stageChange.get("old")).append(" → ").append(stageChange.get("new"));
        } else if (changes.containsKey("segment")) {
            var segmentChange = (Map<String, Object>) changes.get("segment");
            sb.append("Segmento: ").append(segmentChange.get("old")).append(" → ").append(segmentChange.get("new"));
        } else if (changes.containsKey("value")) {
            var valueChange = (Map<String, Object>) changes.get("value");
            sb.append("Valor: ").append(valueChange.get("old")).append(" → ").append(valueChange.get("new"));
        } else if (changes.containsKey("ownerId") || changes.containsKey("accountOwnerId")) {
            sb.append("Asignación cambiada");
        }
        
        return sb.length() > 0 ? sb.toString() : entityName;
    }

    private String getNotificationType(AuditAction action) {
        return switch (action) {
            case CLIENT_STATUS_CHANGED, CLIENT_ASSIGNED_USER_CHANGED, CLIENT_SEGMENT_CHANGED,
                 PROSPECT_CONVERTED, PROSPECT_LOST, PROSPECT_REACTIVATED -> "CLIENT_MOVED";
            case BRANCH_STATUS_CHANGED, BRANCH_ASSIGNED_USER_CHANGED -> "BRANCH_MOVED";
            case OPPORTUNITY_STAGE_CHANGED, OPPORTUNITY_VALUE_CHANGED, OPPORTUNITY_ASSIGNED_USER_CHANGED,
                 OPPORTUNITY_WON, OPPORTUNITY_LOST, OPPORTUNITY_REOPENED -> "OPPORTUNITY_STAGE_CHANGED";
            case CONTACT_ASSIGNED_USER_CHANGED -> "CONTACT_ASSIGNED";
            default -> "CRM_CHANGE";
        };
    }

    private Map<String, Object> serializeClient(Client client) {
        if (client == null) return null;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", client.getId() != null ? client.getId().toString() : null);
        map.put("name", client.getName());
        map.put("status", client.getStatus() != null ? client.getStatus().name() : null);
        map.put("segment", client.getSegment() != null ? client.getSegment().name() : null);
        map.put("accountOwnerId", client.getAccountOwnerId() != null ? client.getAccountOwnerId().toString() : null);
        return map;
    }

    private Map<String, Object> serializeBranch(ClientBranch branch) {
        if (branch == null) return null;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", branch.getId() != null ? branch.getId().toString() : null);
        map.put("name", branch.getName());
        map.put("status", branch.getStatus() != null ? branch.getStatus().name() : null);
        return map;
    }

    private Map<String, Object> serializeOpportunity(ClientOpportunity opp) {
        if (opp == null) return null;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", opp.getId() != null ? opp.getId().toString() : null);
        map.put("title", opp.getTitle());
        map.put("stage", opp.getStage() != null ? opp.getStage().name() : null);
        map.put("value", opp.getValue());
        map.put("ownerId", opp.getOwnerId() != null ? opp.getOwnerId().toString() : null);
        return map;
    }

    private Map<String, Object> serializeContact(ClientContact contact) {
        if (contact == null) return null;
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", contact.getId() != null ? contact.getId().toString() : null);
        map.put("fullName", contact.getFullName());
        map.put("email", contact.getEmail());
        return map;
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        StringBuilder sb = new StringBuilder("{");
        Map<String, Object> map = (Map<String, Object>) obj;
        int i = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":\"");
            sb.append(entry.getValue() != null ? entry.getValue().toString().replace("\"", "'") : "");
            sb.append("\"");
            i++;
        }
        sb.append("}");
        return sb.toString();
    }
}
