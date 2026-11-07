package com.tomanagement.establishment.service;

import com.tomanagement.establishment.entity.EstablishmentKind;

/**
 * Data needed to create an establishment together with its first ADMIN user. {@code approved} says whether the
 * establishment is born approved (superadmin-created) or waits for the superadmin (self-registered).
 */
public record NewEstablishment(String name, EstablishmentKind kind, boolean approved, String adminFullName,
        String adminEmail, String adminPassword) {

    @Override
    public String toString() {
        return "NewEstablishment[kind=" + kind + ", approved=" + approved
                + ", name=***, adminFullName=***, adminEmail=***, adminPassword=***]";
    }
}
