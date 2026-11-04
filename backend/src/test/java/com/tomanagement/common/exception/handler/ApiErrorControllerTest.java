package com.tomanagement.common.exception.handler;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.tomanagement.common.exception.ErrorCode;

import jakarta.servlet.RequestDispatcher;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ApiErrorController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class ApiErrorControllerTest {

    private static final String ERROR_PATH = "/error";

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        Clock clock() {
            return Clock.fixed(Instant.parse("2026-11-04T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Test
    void forbiddenStatusMapsToAccessDenied() throws Exception {
        mockMvc.perform(get(ERROR_PATH).requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 403))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.ACCESS_DENIED.name()));
    }

    @Test
    void badRequestStatusMapsToMalformedRequest() throws Exception {
        mockMvc.perform(get(ERROR_PATH).requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 400))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.MALFORMED_REQUEST.name()));
    }

    @Test
    void missingStatusMapsToInternalError() throws Exception {
        mockMvc.perform(get(ERROR_PATH))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(ErrorCode.INTERNAL_ERROR.name()))
                .andExpect(content().string(not(containsString("999"))));
    }

    @Test
    void unknownStatusMapsToInternalError() throws Exception {
        mockMvc.perform(get(ERROR_PATH).requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 418))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(ErrorCode.INTERNAL_ERROR.name()));
    }
}
