package com.eventsApp.auth.model.command;

import jakarta.validation.constraints.Email;
import lombok.Data;

/** Ustawienia e-mail siedzą w dwóch kafelkach, więc każde pole jedzie osobno — null znaczy „bez zmian”. */
@Data
public class TenantEmailSettingsUpdateCommand {
    private String emailMessage;

    @Email(message = "NOT_EMAIL")
    private String alertEmail;

    private Boolean alertEmailEnabled;
}
