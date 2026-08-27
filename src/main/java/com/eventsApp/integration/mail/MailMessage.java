package com.eventsApp.integration.mail;

public record MailMessage(
        String to,
        String subject,
        String body,
        String attachmentFilename,
        byte[] attachment) {
}
