package com.controltower.app.email.application;

import com.controltower.app.email.domain.*;
import com.controltower.app.support.domain.Ticket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Evaluates email_routing_rules conditions against an inbound email
 * and returns a RoutingResult with the winning rule's actions applied.
 *
 * Rules are ordered by priority ASC; first match wins.
 * Falls back to alias defaults, then mailbox defaults.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailRuleEngineService {

    private final EmailRoutingRuleRepository ruleRepo;

    public record RoutingResult(
        UUID departmentId,
        Ticket.Priority priority,
        UUID assigneeId,
        String[] labels,
        boolean skipTicket,
        String autoReplyTemplateId
    ) {}

    public RoutingResult evaluate(UUID tenantId, EmailRaw raw, EmailAlias alias, EmailMailboxConfig mailbox) {
        UUID aliasId = (alias != null) ? alias.getId() : null;
        List<EmailRoutingRule> rules = ruleRepo.findActiveRulesForAlias(tenantId, aliasId);

        for (EmailRoutingRule rule : rules) {
            if (matches(rule, raw)) {
                log.debug("Rule '{}' (priority {}) matched email {}", rule.getName(), rule.getPriority(), raw.getMessageId());
                return applyActions(rule.getActions(), alias, mailbox);
            }
        }

        // No rule matched — apply defaults
        return buildDefault(alias, mailbox);
    }

    // ── Condition evaluation ──────────────────────────────────────────────────

    private boolean matches(EmailRoutingRule rule, EmailRaw raw) {
        List<Map<String, Object>> conditions = rule.getConditions();
        if (conditions == null || conditions.isEmpty()) return true;

        boolean all = "ALL".equalsIgnoreCase(rule.getMatchMode());
        boolean anyMatched = false;

        for (Map<String, Object> condition : conditions) {
            boolean result = evaluateCondition(condition, raw);
            if (all && !result) return false;
            if (!all && result) anyMatched = true;
        }

        return all || anyMatched;
    }

    private boolean evaluateCondition(Map<String, Object> condition, EmailRaw raw) {
        String field = str(condition, "field");
        String op = str(condition, "op");
        Object value = condition.get("value");

        if (field == null || op == null) return false;

        return switch (field) {
            case "from_email"   -> matchString(op, raw.getFromEmail(), value);
            case "from_domain"  -> matchString(op, extractDomain(raw.getFromEmail()), value);
            case "to_alias"     -> matchString(op, firstTo(raw), value);
            case "subject"      -> matchString(op, raw.getSubject(), value);
            case "body_text"    -> matchString(op, raw.getBodyText(), value);
            case "has_attachment" -> matchBoolean(op, raw.getAttachments() != null && !raw.getAttachments().isEmpty(), value);
            case "is_spam"      -> matchBoolean(op, raw.isSpam(), value);
            case "is_reply"     -> matchBoolean(op, raw.getInReplyTo() != null, value);
            case "hour_of_day"  -> matchHour(op, value);
            case "day_of_week"  -> matchDayOfWeek(op, value);
            default -> {
                log.warn("Unknown condition field: {}", field);
                yield false;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private boolean matchString(String op, String actual, Object value) {
        if (actual == null) return false;
        String lower = actual.toLowerCase();
        return switch (op) {
            case "eq"           -> lower.equals(str(value).toLowerCase());
            case "neq"          -> !lower.equals(str(value).toLowerCase());
            case "contains"     -> lower.contains(str(value).toLowerCase());
            case "not_contains" -> !lower.contains(str(value).toLowerCase());
            case "starts_with"  -> lower.startsWith(str(value).toLowerCase());
            case "ends_with"    -> lower.endsWith(str(value).toLowerCase());
            case "contains_any" -> {
                if (value instanceof List<?> list) {
                    yield list.stream().anyMatch(v -> lower.contains(str(v).toLowerCase()));
                }
                yield false;
            }
            default -> false;
        };
    }

    private boolean matchBoolean(String op, boolean actual, Object value) {
        boolean expected = Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(str(value));
        return "eq".equals(op) ? actual == expected : actual != expected;
    }

    @SuppressWarnings("unchecked")
    private boolean matchHour(String op, Object value) {
        int currentHour = LocalTime.now(ZoneId.of("America/Mexico_City")).getHour();
        if ("between".equals(op) && value instanceof List<?> range && range.size() == 2) {
            int from = toInt(range.get(0));
            int to = toInt(range.get(1));
            return currentHour >= from && currentHour < to;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private boolean matchDayOfWeek(String op, Object value) {
        int day = java.time.LocalDate.now(ZoneId.of("America/Mexico_City")).getDayOfWeek().getValue();
        if ("in".equals(op) && value instanceof List<?> days) {
            return days.stream().anyMatch(d -> toInt(d) == day);
        }
        return false;
    }

    // ── Action application ────────────────────────────────────────────────────

    private RoutingResult applyActions(List<Map<String, Object>> actions, EmailAlias alias, EmailMailboxConfig mailbox) {
        UUID departmentId = alias != null ? alias.getDepartmentId() : mailbox.getDepartmentId();
        Ticket.Priority priority = Ticket.Priority.MEDIUM;
        UUID assigneeId = null;
        List<String> labels = new ArrayList<>();
        boolean skipTicket = false;
        String autoReplyTemplateId = null;

        if (actions != null) {
            for (Map<String, Object> action : actions) {
                String type = str(action, "type");
                Object value = action.get("value");
                switch (type) {
                    case "set_department"  -> departmentId = parseUuid(value);
                    case "set_priority"    -> priority = parsePriority(value);
                    case "assign_to"       -> assigneeId = parseUuid(value);
                    case "add_label"       -> labels.add(str(value));
                    case "skip_ticket"     -> skipTicket = Boolean.TRUE.equals(value);
                    case "auto_reply"      -> autoReplyTemplateId = str(action, "template_id");
                }
            }
        }

        return new RoutingResult(departmentId, priority, assigneeId,
            labels.toArray(new String[0]), skipTicket, autoReplyTemplateId);
    }

    private RoutingResult buildDefault(EmailAlias alias, EmailMailboxConfig mailbox) {
        UUID departmentId = (alias != null && alias.getDepartmentId() != null)
            ? alias.getDepartmentId()
            : mailbox.getDepartmentId();
        return new RoutingResult(departmentId, Ticket.Priority.MEDIUM, null, new String[0], false, null);
    }

    // ── Util ──────────────────────────────────────────────────────────────────

    private String str(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private String str(Object val) {
        return val != null ? val.toString() : "";
    }

    private UUID parseUuid(Object val) {
        try { return UUID.fromString(str(val)); } catch (Exception e) { return null; }
    }

    private Ticket.Priority parsePriority(Object val) {
        try { return Ticket.Priority.valueOf(str(val).toUpperCase()); }
        catch (Exception e) { return Ticket.Priority.MEDIUM; }
    }

    private String extractDomain(String email) {
        if (email == null || !email.contains("@")) return null;
        return email.substring(email.indexOf('@') + 1).toLowerCase();
    }

    private String firstTo(EmailRaw raw) {
        if (raw.getToEmail() == null || raw.getToEmail().length == 0) return null;
        return raw.getToEmail()[0];
    }

    private int toInt(Object val) {
        try { return Integer.parseInt(str(val)); } catch (Exception e) { return -1; }
    }
}
