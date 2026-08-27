package com.eventsApp.integration.mail.model.dto;

import com.eventsApp.integration.mail.model.MailConnectionStatus;
import com.eventsApp.integration.mail.model.MailProvider;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MailConnectionStatusDTO {
    private boolean connected;
    private MailProvider provider;
    private String email;
    private MailConnectionStatus status;
    private LocalDateTime connectedAt;
}
