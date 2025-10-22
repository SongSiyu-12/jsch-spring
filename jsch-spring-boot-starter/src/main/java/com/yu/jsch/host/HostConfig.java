package com.yu.jsch.host;

import com.yu.jsch.AuthType;
import com.yu.jsch.KnownHostsMode;
import lombok.Builder;
import lombok.Value;

/**
 * Mutable host configuration used at runtime to resolve SSH connection details.
 * Contains only the fields needed to establish an SSH session and execute commands/SFTP.
 */
@Value
@Builder(toBuilder = true)
public class HostConfig {
    String host;
    int port;
    String username;

    Auth auth;
    KnownHosts knownHosts;

    int connectTimeoutMillis;
    int readTimeoutMillis;

    Long version;

    /**
     * A stable key to identify this host for pooling purposes (excluding secrets).
     * Format: host:port:username
     */
    public String stableKey() {
        return host + ":" + port + ":" + username;
    }

    /**
     * Clear sensitive data held in memory.
     */
    public void clearSensitive() {
        if (auth != null) {
            auth.clearSensitive();
        }
    }

    @Value
    @Builder(toBuilder = true)
    public static class Auth {
        AuthType type;
        char[] password;
        String privateKeyPath;
        String privateKey;
        char[] passphrase;

        public void clearSensitive() {
            if (password != null) zero(password);
            if (passphrase != null) zero(passphrase);
        }

        private void zero(char[] arr) {
            for (int i = 0; i < arr.length; i++) arr[i] = '\0';
        }
    }

    @Value
    @Builder(toBuilder = true)
    public static class KnownHosts {
        KnownHostsMode mode;
        String path;
    }
}
