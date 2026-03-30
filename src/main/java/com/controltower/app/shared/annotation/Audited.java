package com.controltower.app.shared.annotation;

import java.lang.annotation.*;

/**
 * Marks a service method for automatic audit logging.
 * Applied by AuditAspect (Feature 5) which intercepts annotated methods
 * and records who did what, when, and with what result.
 *
 * Usage:
 * <pre>
 *   {@literal @}Audited(action = "SUSPEND", resource = "License")
 *   public void suspendLicense(UUID licenseId) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audited {

    /** The action name to record (e.g., "CREATE", "SUSPEND"). */
    String action() default "";

    /** The resource type being acted upon (e.g., "License", "Ticket"). */
    String resource() default "";
}
