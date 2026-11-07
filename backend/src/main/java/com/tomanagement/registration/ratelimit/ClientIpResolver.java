package com.tomanagement.registration.ratelimit;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

/**
 * Finds the client address: the first X-Forwarded-For entry if it is a literal IPv4/IPv6 address (the app runs behind
 * a proxy), else the socket address. Values are checked syntactically, never resolved through DNS.
 */
@Component
public class ClientIpResolver {

    private static final Pattern IPV4 = Pattern.compile(
            "(?:(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)");
    private static final Pattern IPV6_CHARACTERS = Pattern.compile("[0-9a-fA-F:][0-9a-fA-F:.]{1,44}");

    public String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader(ForwardedHeader.X_FORWARDED_FOR.headerName());
        if (forwarded != null) {
            String first = forwarded.split(",", 2)[0].trim();
            if (isIpLiteral(first)) {
                return first;
            }
        }
        return request.getRemoteAddr();
    }

    private static boolean isIpLiteral(String value) {
        if (IPV4.matcher(value).matches()) {
            return true;
        }
        // Only hex digits, colons and dots, with at least one colon: the JDK parses it as a literal, no DNS lookup.
        if (!IPV6_CHARACTERS.matcher(value).matches() || value.indexOf(':') < 0) {
            return false;
        }
        try {
            InetAddress.getByName(value);
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
