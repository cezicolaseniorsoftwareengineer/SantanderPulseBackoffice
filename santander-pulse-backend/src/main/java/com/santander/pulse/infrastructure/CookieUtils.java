package com.santander.pulse.infrastructure;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.SerializationUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * Utility helpers to manage HTTP cookies used during OAuth2 flows.
 */
public final class CookieUtils {

    private CookieUtils() {
        // Utility class
    }

    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return Optional.empty();
        }

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) {
                return Optional.of(cookie);
            }
        }
        return Optional.empty();
    }

    public static void addCookie(HttpServletResponse response, String name, String value, int maxAgeSeconds) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // For local development; enable HTTPS in production
        cookie.setMaxAge(maxAgeSeconds);
        response.addCookie(cookie);
    }

    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return;
        }

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) {
                cookie.setValue("");
                cookie.setPath("/");
                cookie.setMaxAge(0);
                response.addCookie(cookie);
            }
        }
    }

    public static String serialize(Object object) {
        byte[] bytes = SerializationUtils.serialize(object);
        if (bytes == null) {
            return "";
        }
        return Base64.getUrlEncoder().encodeToString(bytes);
    }

    public static <T> T deserialize(String value, Class<T> clazz) {
        byte[] decodedBytes = Base64.getUrlDecoder().decode(value.getBytes(StandardCharsets.UTF_8));
        try (ByteArrayInputStream bais = new ByteArrayInputStream(decodedBytes);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            Object deserialized = ois.readObject();
            if (clazz.isInstance(deserialized)) {
                return clazz.cast(deserialized);
            }
            throw new IllegalStateException("Failed to deserialize cookie to " + clazz.getSimpleName());
        } catch (IOException | ClassNotFoundException ex) {
            throw new IllegalStateException("Failed to deserialize OAuth2 authorization request", ex);
        }
    }
}