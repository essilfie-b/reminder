package com.example.reminder.email;

import lombok.Getter;

@Getter
public enum EmailTemplateName {

    ACTIVATE_ACCOUNT("activate_account");
    private String name;

    private EmailTemplateName(String name) {
        this.name = name;
    }
}
