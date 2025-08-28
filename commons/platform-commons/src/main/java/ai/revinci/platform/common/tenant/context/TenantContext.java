package ai.revinci.platform.common.tenant.context;

import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

import lombok.extern.slf4j.Slf4j;

import ai.revinci.platform.common.enums.Key;
import ai.revinci.platform.common.error.CommonErrors;
import ai.revinci.platform.common.exception.ServiceException;

/**
 * Implementation class that holds the tenant realm in a thread-local variable. This is available for the entire
 * request-response chain.
 */
@Slf4j
public final class TenantContext {
    /** Current tenant identifier that will be stored as a thread-local and available for the current thread. */
    private static final InheritableThreadLocal<TenantRealm> TENANT = new InheritableThreadLocal<>();

    /**
     * Private constructor.
     */
    private TenantContext() {
        throw new IllegalStateException("Cannot create instances of this class");
    }

    /**
     * This method returns an instance of type {@link TenantRealm} that is stored in the thread-local variable.
     *
     * @return Tenant realm name.
     */
    public static TenantRealm get() {
        return TenantContext.TENANT.get();
    }

    /**
     * This method uses the tenant context stored in the thread-local variable, retrieves the realm name from the
     * context and returns it.
     *
     * @return Tenant realm name.
     */
    public static String realm() {
        final TenantRealm tenantRealm = TenantContext.TENANT.get();
        if (Objects.isNull(tenantRealm)) {
            return null;
        }

        return tenantRealm.getRealm();
    }

    /**
     * This method uses the tenant context stored in the thread-local variable, retrieves the realm name from the
     * context and returns it. If the realm name is not available, it throws an exception.
     *
     * @return Tenant realm name.
     */
    public static String realmOrThrow() {
        final String realm = TenantContext.realm();
        if (StringUtils.isBlank(realm)) {
            throw ServiceException.of(CommonErrors.MISSING_REALM);
        }
        return realm;
    }

    /**
     * This method uses the tenant context stored in the thread-local variable, retrieves the tenant-identifier from the
     * context and returns it.
     *
     * @return Unique identifier of the tenant.
     */
    public static UUID tenantId() {
        final TenantRealm tenantRealm = TenantContext.TENANT.get();
        if (Objects.isNull(tenantRealm)) {
            return null;
        }

        return tenantRealm.getTenantId();
    }

    /**
     * This method uses the tenant context stored in the thread-local variable, retrieves the tenant-identifier from the
     * context and returns it. If the tenant-identifier is not available, it throws an exception.
     *
     * @return Unique identifier of the tenant.
     */
    public static UUID tenantIdOrThrow() {
        final UUID tenantId = TenantContext.tenantId();
        if (Objects.isNull(tenantId)) {
            throw ServiceException.of(CommonErrors.MISSING_TENANT_ID);
        }
        return tenantId;
    }



    /**
     * This method sets the tenant realm in the thread-local variable.
     *
     * @param tenantRealm Instance of type {@link TenantRealm} that holds the tenant realm or tenant identifier of
     *                    both.
     */
    public static void set(final TenantRealm tenantRealm) {
        TenantContext.TENANT.set(tenantRealm);
        TenantContext.setMDC(tenantRealm);
    }

    /**
     * This method refreshes the tenant realm in the thread-local variable.
     *
     * @param tenantRealm Instance of type {@link TenantRealm} that holds the tenant realm or tenant identifier of
     *                    both.
     */
    public static void refresh(final TenantRealm tenantRealm) {
        // Clear the current one before setting the new one.
        TenantContext.clear();
        TenantContext.set(tenantRealm);
    }

    /**
     * This method removes the value set in the thread-local variable.
     */
    public static void clear() {
        TenantContext.TENANT.remove();
        TenantContext.clearMDC();
    }

    /**
     * This method sets the {@code realm} in the MDC context.
     *
     * @param tenantRealm Instance containing the {@code realm} detials, which will be added
     *                    to the MDC.
     */
    public static void setMDC(final TenantRealm tenantRealm) {
        final String realm = tenantRealm.getRealm();
        if (StringUtils.isNotBlank(realm)) {
            final String key = Key.REALM.value();
            try {
                MDC.put(key, realm);
            } catch (final Exception e) {
                TenantContext.LOGGER.error("Realm: {}. Unable to add {} to MDC. Error: {}", realm, key, e.getMessage());
            }
        }

        final UUID tenantId = tenantRealm.getTenantId();
        if (Objects.nonNull(tenantId)) {
            final String key = Key.TENANT_ID.value();
            try {
                MDC.put(key, tenantId.toString());
            } catch (final Exception e) {
                TenantContext.LOGGER.error("Realm: {}. Tenant: {}. Unable to add {} to MDC. Error: {}", realm, tenantId,
                                           key, e.getMessage());
            }
        }

    }

    /**
     * This method clears / removes the {@code realm} from the MDC context.
     */
    public static void clearMDC() {
        MDC.remove(Key.REALM.value());
        MDC.remove(Key.TENANT_ID.value());
    }
}
