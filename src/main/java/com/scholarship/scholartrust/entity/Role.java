package com.scholarship.scholartrust.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Role {
    ROLE_STUDENT,
    ROLE_ADMIN;

    @JsonCreator
    public static Role fromString(String value) {
        if (value == null) return null;
        String v = value.trim().toUpperCase();
        if (v.equals("STUDENT") || v.equals("ROLE_STUDENT")) return ROLE_STUDENT;
        if (v.equals("ADMIN") || v.equals("ROLE_ADMIN")) return ROLE_ADMIN;
        throw new IllegalArgumentException("Unknown role: " + value + ". Expected ROLE_STUDENT or ROLE_ADMIN");
    }
}