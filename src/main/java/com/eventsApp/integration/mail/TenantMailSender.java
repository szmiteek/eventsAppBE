package com.eventsApp.integration.mail;

/**
 * Sends mail on behalf of the tenant resolved from the current security context.
 * The caller never supplies a tenant id or any credentials.
 */
public interface TenantMailSender {

    void send(MailMessage message);
}
