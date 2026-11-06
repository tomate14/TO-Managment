package com.tomanagement.auth.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;

import com.tomanagement.TestcontainersConfiguration;
import com.tomanagement.auth.config.JwtProperties;
import com.tomanagement.common.exception.ErrorCode;
import com.tomanagement.user.entity.Role;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class SecurityIntegrationTest {

    private static final AuthenticatedUser THERAPIST =
            new AuthenticatedUser(1L, "ana@consultorio.com", "Ana Pérez", Role.TERAPEUTA, 1L);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    void requestWithoutTokenGets401AuthenticationRequired() throws Exception {
        mockMvc.perform(get("/api/test/secured"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.AUTHENTICATION_REQUIRED.name()))
                .andExpect(jsonPath("$.message").value(ErrorCode.AUTHENTICATION_REQUIRED.getMessage()));
    }

    @Test
    void requestWithValidTokenReachesTheEndpointWithThePrincipal() throws Exception {
        mockMvc.perform(get("/api/test/secured").header(HttpHeaders.AUTHORIZATION, bearer(tokenProvider.issue(THERAPIST))))
                .andExpect(status().isOk())
                .andExpect(content().string(THERAPIST.email()));
    }

    @Test
    void expiredTokenGets401TokenExpired() throws Exception {
        Clock past = Clock.offset(Clock.systemUTC(), Duration.ofMillis(-jwtProperties.expirationMs() - 60_000));
        IssuedToken expired = new JwtTokenProvider(jwtProperties, past.withZone(ZoneOffset.UTC)).issue(THERAPIST);

        mockMvc.perform(get("/api/test/secured").header(HttpHeaders.AUTHORIZATION, bearer(expired)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.TOKEN_EXPIRED.name()));
    }

    @Test
    void malformedTokenGets401AuthenticationRequired() throws Exception {
        mockMvc.perform(get("/api/test/secured").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.AUTHENTICATION_REQUIRED.name()));
    }

    @Test
    void nonBearerAuthorizationHeaderIsIgnored() throws Exception {
        mockMvc.perform(get("/api/test/secured").header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.AUTHENTICATION_REQUIRED.name()));
    }

    @Test
    void methodSecurityDenialGets403AccessDenied() throws Exception {
        mockMvc.perform(get("/api/test/admin-only").header(HttpHeaders.AUTHORIZATION, bearer(tokenProvider.issue(THERAPIST))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.ACCESS_DENIED.name()));
    }

    @Test
    void healthStaysPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void unknownEndpointWithoutTokenGets401NotLeakingRoutes() throws Exception {
        mockMvc.perform(get("/api/no-existe"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.AUTHENTICATION_REQUIRED.name()));
    }

    @Test
    void corsPreflightFromAllowedOriginIsAccepted() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
    }

    @Test
    void corsPreflightFromUnknownOriginIsRejected() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden());
    }

    private static String bearer(IssuedToken token) {
        return "Bearer " + token.value();
    }
}
