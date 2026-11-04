package com.tomanagement.common.exception.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.tomanagement.common.exception.ErrorCode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ExceptionProbeController.class)
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    private static final String FIXED_INSTANT = "2026-11-04T12:00:00Z";

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse(FIXED_INSTANT), ZoneOffset.UTC);
        }
    }

    @Test
    void resourceNotFoundMapsTo404WithUniformBody() throws Exception {
        mockMvc.perform(get("/test/errors/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_NOT_FOUND.name()))
                .andExpect(jsonPath("$.message").value(ErrorCode.RESOURCE_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.timestamp").value(FIXED_INSTANT))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void duplicateResourceMapsTo409() throws Exception {
        mockMvc.perform(get("/test/errors/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.DUPLICATE_RESOURCE.name()));
    }

    @Test
    void businessRuleMapsTo422() throws Exception {
        mockMvc.perform(get("/test/errors/business"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(ErrorCode.BUSINESS_RULE_VIOLATION.name()));
    }

    @Test
    void authenticationFailureMapsTo401WithGenericMessage() throws Exception {
        mockMvc.perform(get("/test/errors/authentication"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.AUTHENTICATION_FAILED.name()))
                .andExpect(jsonPath("$.message").value("Email o contraseña incorrectos."));
    }

    @Test
    void accessDeniedMapsTo403() throws Exception {
        mockMvc.perform(get("/test/errors/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.ACCESS_DENIED.name()));
    }

    @Test
    void unexpectedExceptionIsTranslatedToGenericInternalError() throws Exception {
        mockMvc.perform(get("/test/errors/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(ErrorCode.INTERNAL_ERROR.name()))
                .andExpect(jsonPath("$.message").value(ErrorCode.INTERNAL_ERROR.getMessage()))
                .andExpect(content().string(not(containsString("secret"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    @Test
    void internalSystemExceptionIsGenericAndLogsCause(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/test/errors/internal"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(ErrorCode.INTERNAL_ERROR.name()))
                .andExpect(content().string(not(containsString("secret"))));
        assertThat(output.getAll()).contains("IllegalStateException");
    }

    @Test
    void methodParameterConstraintViolationReturns400() throws Exception {
        mockMvc.perform(get("/test/errors/bounded").param("value", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()));
    }

    @Test
    void invalidBodyReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/test/errors/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_ERROR.name()))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                .andExpect(jsonPath("$.fieldErrors[0].message").isNotEmpty());
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mockMvc.perform(post("/test/errors/validated")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.MALFORMED_REQUEST.name()));
    }

    @Test
    void wrongPathVariableTypeReturns400() throws Exception {
        mockMvc.perform(get("/test/errors/typed/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.MALFORMED_REQUEST.name()));
    }

    @Test
    void missingRequiredParameterReturns400() throws Exception {
        mockMvc.perform(get("/test/errors/param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.MALFORMED_REQUEST.name()));
    }

    @Test
    void missingRequiredHeaderReturns400() throws Exception {
        mockMvc.perform(get("/test/errors/header"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.MALFORMED_REQUEST.name()));
    }

    @Test
    void unsupportedContentTypeReturns415() throws Exception {
        mockMvc.perform(post("/test/errors/validated")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("name"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value(ErrorCode.UNSUPPORTED_MEDIA_TYPE.name()));
    }

    @Test
    void unknownEndpointReturns404() throws Exception {
        mockMvc.perform(get("/api/no/such/path"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.ENDPOINT_NOT_FOUND.name()));
    }

    @Test
    void wrongHttpMethodReturns405() throws Exception {
        mockMvc.perform(delete("/test/errors/not-found"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value(ErrorCode.METHOD_NOT_ALLOWED.name()));
    }
}
