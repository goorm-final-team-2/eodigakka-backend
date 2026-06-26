package com.eodigakka.domain.appointment;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.guest-cookie")
public record GuestCookieProperties(
    String name, String path, boolean secure, String sameSite, long maxAgeDays) {}
