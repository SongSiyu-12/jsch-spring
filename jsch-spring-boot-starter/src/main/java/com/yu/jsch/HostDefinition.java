package com.yu.jsch;

import lombok.Builder;
import lombok.Value;

import java.time.Duration;

/**
 * Immutable value object representing a fully-resolved SSH host configuration.
 */
@Value
@Builder
public class HostDefinition {
    String alias;
    String host;
    int port;
    String username;
    Authentication authentication;
    KnownHosts knownHosts;
    Retry retry;
    Pool pool;
    Timeouts timeouts;

    @Value
    @Builder
    public static class Authentication {
        AuthType type;
        String password;
        String privateKeyPath;
        String privateKey;
        String passphrase;
    }

    @Value
    @Builder
    public static class KnownHosts {
        KnownHostsMode mode;
        String path;
    }

    @Value
    @Builder
    public static class Retry {
        boolean enabled;
        int maxAttempts;
        Duration delay;
    }

    @Value
    @Builder
    public static class Pool {
        boolean enabled;
        int maxTotal;
        int maxIdle;
        int minIdle;
        Duration maxWait;
    }

    @Value
    @Builder
    public static class Timeouts {
        Duration connect;
        Duration authentication;
        Duration session;
        Duration read;
    }
}
