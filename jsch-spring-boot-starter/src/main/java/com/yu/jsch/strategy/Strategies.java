package com.yu.jsch.strategy;

import com.yu.jsch.AuthType;
import com.yu.jsch.HostDefinition;
import com.yu.jsch.KnownHostsMode;

import java.io.IOException;
import java.time.Duration;

/**
 * Factory methods to build strategies from a {@link HostDefinition} produced by configuration properties.
 */
public final class Strategies {

    private Strategies() {
    }

    public static AuthStrategy authFrom(HostDefinition host) {
        HostDefinition.Authentication auth = host.getAuthentication();
        if (auth.getType() == AuthType.PASSWORD) {
            if (auth.getPassword() == null) {
                throw new IllegalArgumentException("Password must be provided for password authentication");
            }
            return new PasswordAuthStrategy(auth.getPassword());
        }
        if (auth.getType() == AuthType.PUBLIC_KEY) {
            try {
                if (auth.getPrivateKey() != null && !auth.getPrivateKey().isBlank()) {
                    return PrivateKeyAuthStrategy.fromInline(auth.getPrivateKey(), auth.getPassphrase());
                }
                if (auth.getPrivateKeyPath() != null && !auth.getPrivateKeyPath().isBlank()) {
                    return PrivateKeyAuthStrategy.fromPath(auth.getPrivateKeyPath(), auth.getPassphrase());
                }
            } catch (IOException e) {
                throw new IllegalArgumentException("Failed to read private key from path: " + auth.getPrivateKeyPath(), e);
            }
            throw new IllegalArgumentException("Private key (inline or path) must be provided for public key authentication");
        }
        throw new IllegalArgumentException("Unsupported auth type: " + auth.getType());
    }

    public static HostKeyVerificationStrategy hostKeyVerificationFrom(HostDefinition host) {
        HostDefinition.KnownHosts kh = host.getKnownHosts();
        KnownHostsMode mode = kh.getMode();
        if (mode == KnownHostsMode.STRICT) {
            String path = kh.getPath();
            if (path == null || path.isBlank()) {
                throw new IllegalArgumentException("known_hosts path must be provided for STRICT mode");
            }
            return new StrictKnownHostsVerificationStrategy(path);
        }
        // For ACCEPT_NEW and OFF we behave leniently and accept all
        return new LenientHostKeyVerificationStrategy();
    }

    public static RetryStrategy retryFrom(HostDefinition host) {
        HostDefinition.Retry r = host.getRetry();
        if (!r.isEnabled() || r.getMaxAttempts() <= 1) {
            return new NoRetryStrategy();
        }
        Duration d = r.getDelay();
        long baseDelayMs = d != null ? d.toMillis() : 200L;
        return ExponentialBackoffRetryStrategy.builder()
                .maxAttempts(r.getMaxAttempts() - 1) // retries after the first attempt
                .baseDelayMillis(baseDelayMs)
                .multiplier(2.0d)
                .build();
    }
}
