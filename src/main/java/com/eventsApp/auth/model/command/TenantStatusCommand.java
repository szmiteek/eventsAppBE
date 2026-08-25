package com.eventsApp.auth.model.command;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TenantStatusCommand {

    @NotNull
    private Boolean active;
}
