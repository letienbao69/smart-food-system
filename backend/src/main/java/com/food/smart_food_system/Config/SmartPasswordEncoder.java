package com.food.smart_food_system.Config;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * BCrypt-only password encoder.
 *
 * NOTE: The previous "SmartPasswordEncoder" allowed plaintext password matching as
 * a fallback. That has been removed: it is a security risk because anyone with DB
 * access could insert a plaintext password and pass authentication.
 *
 * If you imported the legacy SQL dump where the admin password "1234567" is stored
 * in plaintext, run the helper at /api/auth/migrate-legacy-passwords (ADMIN only)
 * or update the DB row manually with a BCrypt hash, e.g.:
 *
 *   UPDATE users SET password = '$2a$10$...' WHERE email = 'admin@example.com';
 */
@Component
public class SmartPasswordEncoder implements PasswordEncoder {

    private final BCryptPasswordEncoder delegate = new BCryptPasswordEncoder();

    @Override
    public String encode(CharSequence rawPassword) {
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        // Only accept BCrypt-encoded passwords. Anything that does not look like
        // a BCrypt hash is treated as invalid (defense in depth in case legacy
        // plaintext rows were not migrated).
        if (!encodedPassword.startsWith("$2a$")
                && !encodedPassword.startsWith("$2b$")
                && !encodedPassword.startsWith("$2y$")) {
            return false;
        }
        try {
            return delegate.matches(rawPassword, encodedPassword);
        } catch (Exception e) {
            return false;
        }
    }
}
