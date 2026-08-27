package com.eventsApp.auth.model.command;

import lombok.Data;

@Data
public class TenantEmailSettingsUpdateCommand {
    private String emailMessage;
}
