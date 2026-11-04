package com.tomanagement.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LoginRequestTest {

    @Test
    void loginRequestToStringMasksPassword() {
        LoginRequest request = new LoginRequest("ana@consultorio.com", "super-secreta-123");

        assertThat(request.toString())
                .doesNotContain("super-secreta-123")
                .contains("ana@consultorio.com");
    }
}
