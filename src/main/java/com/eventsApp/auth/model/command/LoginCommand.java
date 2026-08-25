package com.eventsApp.auth.model.command;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginCommand {

    @NotBlank
    private String email;

    @NotBlank
    private String password;
}
