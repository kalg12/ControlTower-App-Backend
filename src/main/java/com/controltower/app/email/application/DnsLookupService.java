package com.controltower.app.email.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;

/**
 * Looks up SPF, DKIM, and DMARC TXT records for a domain.
 * Uses the JVM's built-in JNDI DNS resolver — no extra dependencies needed.
 */
@Slf4j
@Service
public class DnsLookupService {

    /** Common DKIM selectors used by hosting providers (cPanel, Hostinger, Google, etc.). */
    public static final List<String> COMMON_SELECTORS = List.of(
            "default", "mail", "dkim", "k1", "s1", "s2",
            "google", "zoho", "selector1", "selector2"
    );

    public record SpfResult(boolean found, String record) {}
    public record DkimResult(boolean found, String selector, String record) {}
    public record DmarcResult(boolean found, String policy, String record) {}

    // ── SPF ───────────────────────────────────────────────────────────────────

    public SpfResult lookupSpf(String domain) {
        for (String txt : queryTxt(domain)) {
            if (txt.startsWith("v=spf1")) {
                return new SpfResult(true, txt);
            }
        }
        return new SpfResult(false, null);
    }

    // ── DKIM ──────────────────────────────────────────────────────────────────

    /** Checks a specific selector. Returns empty if not found. */
    public Optional<DkimResult> lookupDkim(String domain, String selector) {
        String dkimDomain = selector + "._domainkey." + domain;
        List<String> records = queryTxt(dkimDomain);
        return records.stream()
                .filter(r -> r.contains("v=DKIM1") || r.contains("k=rsa") || r.contains("p="))
                .findFirst()
                .map(r -> new DkimResult(true, selector, truncate(r, 120)));
    }

    /** Scans common selectors and returns the first match found. */
    public Optional<DkimResult> findAnyDkim(String domain) {
        for (String selector : COMMON_SELECTORS) {
            Optional<DkimResult> result = lookupDkim(domain, selector);
            if (result.isPresent()) return result;
        }
        return Optional.empty();
    }

    // ── DMARC ─────────────────────────────────────────────────────────────────

    public DmarcResult lookupDmarc(String domain) {
        String dmarcDomain = "_dmarc." + domain;
        for (String txt : queryTxt(dmarcDomain)) {
            if (txt.startsWith("v=DMARC1")) {
                String policy = extractTag(txt, "p");
                return new DmarcResult(true, policy, txt);
            }
        }
        return new DmarcResult(false, null, null);
    }

    // ── DNS query ─────────────────────────────────────────────────────────────

    private List<String> queryTxt(String fqdn) {
        List<String> results = new ArrayList<>();
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            env.put("com.sun.jndi.dns.timeout.initial", "3000");
            env.put("com.sun.jndi.dns.timeout.retries", "2");
            InitialDirContext ctx = new InitialDirContext(env);
            Attributes attrs = ctx.getAttributes("dns:/" + fqdn, new String[]{"TXT"});
            Attribute attr = attrs.get("TXT");
            if (attr != null) {
                NamingEnumeration<?> vals = attr.getAll();
                while (vals.hasMore()) {
                    String val = vals.next().toString().replace("\"", "");
                    results.add(val);
                }
            }
        } catch (Exception e) {
            log.trace("[DNS] No TXT record for {}: {}", fqdn, e.getMessage());
        }
        return results;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String extractTag(String record, String tag) {
        for (String part : record.split(";")) {
            String trimmed = part.trim();
            if (trimmed.startsWith(tag + "=")) {
                return trimmed.substring(tag.length() + 1).trim();
            }
        }
        return "none";
    }

    private static String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "…" : s;
    }
}
