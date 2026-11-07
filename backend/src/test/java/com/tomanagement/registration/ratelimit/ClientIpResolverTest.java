package com.tomanagement.registration.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientIpResolverTest {

    @Test
    void prefersTheFirstForwardedAddress() {
        MockHttpServletRequest r = new MockHttpServletRequest();
        r.addHeader("X-Forwarded-For", "203.0.113.7, 10.0.0.1");
        r.setRemoteAddr("10.0.0.2");
        assertThat(new ClientIpResolver().resolve(r)).isEqualTo("203.0.113.7");
    }

    @Test
    void acceptsIpv6Literals() {
        MockHttpServletRequest r = new MockHttpServletRequest();
        r.addHeader("X-Forwarded-For", "2001:db8::1");
        r.setRemoteAddr("10.0.0.2");
        assertThat(new ClientIpResolver().resolve(r)).isEqualTo("2001:db8::1");
    }

    @Test
    void fallsBackToTheRemoteAddressWhenTheForwardedValueIsNotAnIp() {
        for (String bad : new String[] {"not-an-ip", "999.1.1.1", "example.com", "dead.beef", "::zz", "1".repeat(500)}) {
            MockHttpServletRequest r = new MockHttpServletRequest();
            r.addHeader("X-Forwarded-For", bad);
            r.setRemoteAddr("10.0.0.2");
            assertThat(new ClientIpResolver().resolve(r)).as(bad).isEqualTo("10.0.0.2");
        }
    }

    @Test
    void fallsBackToTheRemoteAddress() {
        MockHttpServletRequest r = new MockHttpServletRequest();
        r.setRemoteAddr("10.0.0.2");
        assertThat(new ClientIpResolver().resolve(r)).isEqualTo("10.0.0.2");
    }
}
