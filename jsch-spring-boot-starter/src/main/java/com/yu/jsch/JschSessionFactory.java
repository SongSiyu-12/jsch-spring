package com.yu.jsch;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.yu.jsch.auth.AuthStrategy;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Factory for creating configured JSch Session instances.
 * Applies timeouts, compression, keepalive, known_hosts, and authentication strategies.
 */
public class JschSessionFactory {

    private final Supplier<JSch> jschSupplier;
    private final String host;
    private final int port;
    private final String username;

    private final int connectTimeoutMillis;
    private final int socketTimeoutMillis;

    private final boolean enableCompression;
    private final int compressionLevel;

    private final int serverAliveIntervalMillis;
    private final int serverAliveCountMax;

    private final KnownHostsMode knownHostsMode;
    private final String knownHostsPath;

    private final AuthStrategy authStrategy;

    private JschSessionFactory(Builder builder) {
        this.jschSupplier = builder.jschSupplier;
        this.host = builder.host;
        this.port = builder.port;
        this.username = builder.username;
        this.connectTimeoutMillis = builder.connectTimeoutMillis;
        this.socketTimeoutMillis = builder.socketTimeoutMillis;
        this.enableCompression = builder.enableCompression;
        this.compressionLevel = builder.compressionLevel;
        this.serverAliveIntervalMillis = builder.serverAliveIntervalMillis;
        this.serverAliveCountMax = builder.serverAliveCountMax;
        this.knownHostsMode = builder.knownHostsMode;
        this.knownHostsPath = builder.knownHostsPath;
        this.authStrategy = builder.authStrategy;
    }

    public Session createAndConnect() throws JSchException {
        JSch jsch = Objects.requireNonNull(jschSupplier.get(), "JSch supplier returned null");
        if (knownHostsPath != null && !knownHostsPath.isBlank()) {
            jsch.setKnownHosts(knownHostsPath);
        }
        Session session = null;
        try {
            session = jsch.getSession(username, host, port);
            if (socketTimeoutMillis > 0) {
                session.setTimeout(socketTimeoutMillis);
            }
            if (serverAliveIntervalMillis > 0) {
                session.setServerAliveInterval(serverAliveIntervalMillis);
            }
            if (serverAliveCountMax > 0) {
                session.setServerAliveCountMax(serverAliveCountMax);
            }

            // Known hosts / strict host key checking behavior
            if (knownHostsMode != null) {
                switch (knownHostsMode) {
                    case STRICT -> session.setConfig("StrictHostKeyChecking", "yes");
                    case ACCEPT_NEW, OFF -> session.setConfig("StrictHostKeyChecking", "no");
                }
            }

            if (enableCompression) {
                session.setConfig("compression.s2c", "zlib@openssh.com,zlib,none");
                session.setConfig("compression.c2s", "zlib@openssh.com,zlib,none");
                session.setConfig("compression_level", String.valueOf(Math.max(0, Math.min(9, compressionLevel))));
            }

            if (authStrategy != null) {
                authStrategy.configure(jsch, session);
            }

            if (connectTimeoutMillis > 0) {
                session.connect(connectTimeoutMillis);
            } else {
                session.connect();
            }
            return session;
        } catch (JSchException ex) {
            if (session != null) {
                try {
                    session.disconnect();
                } catch (Throwable ignore) {
                }
            }
            throw ex;
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUsername() {
        return username;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Supplier<JSch> jschSupplier = JSch::new;
        private String host;
        private int port = 22;
        private String username;
        private int connectTimeoutMillis = 5000;
        private int socketTimeoutMillis = 30000;
        private boolean enableCompression = false;
        private int compressionLevel = 6;
        private int serverAliveIntervalMillis = 15000;
        private int serverAliveCountMax = 3;
        private KnownHostsMode knownHostsMode = KnownHostsMode.STRICT;
        private String knownHostsPath = System.getProperty("user.home", "") + "/.ssh/known_hosts";
        private AuthStrategy authStrategy;

        public Builder jsch(Supplier<JSch> supplier) {
            this.jschSupplier = Objects.requireNonNull(supplier);
            return this;
        }

        public Builder host(String host) {
            if (host == null || host.trim().isEmpty()) {
                throw new IllegalArgumentException("host cannot be null or empty");
            }
            this.host = host.trim();
            return this;
        }

        public Builder port(int port) {
            if (port <= 0 || port > 65535) {
                throw new IllegalArgumentException("port must be between 1 and 65535");
            }
            this.port = port;
            return this;
        }

        public Builder username(String username) {
            if (username == null || username.trim().isEmpty()) {
                throw new IllegalArgumentException("username cannot be null or empty");
            }
            this.username = username.trim();
            return this;
        }

        public Builder connectTimeoutMillis(int connectTimeoutMillis) {
            if (connectTimeoutMillis < 0) {
                throw new IllegalArgumentException("connectTimeoutMillis must be >= 0");
            }
            this.connectTimeoutMillis = connectTimeoutMillis;
            return this;
        }

        public Builder socketTimeoutMillis(int socketTimeoutMillis) {
            if (socketTimeoutMillis < 0) {
                throw new IllegalArgumentException("socketTimeoutMillis must be >= 0");
            }
            this.socketTimeoutMillis = socketTimeoutMillis;
            return this;
        }

        public Builder enableCompression(boolean enableCompression) {
            this.enableCompression = enableCompression;
            return this;
        }

        public Builder compressionLevel(int compressionLevel) {
            this.compressionLevel = compressionLevel;
            return this;
        }

        public Builder serverAliveIntervalMillis(int serverAliveIntervalMillis) {
            this.serverAliveIntervalMillis = serverAliveIntervalMillis;
            return this;
        }

        public Builder serverAliveCountMax(int serverAliveCountMax) {
            this.serverAliveCountMax = serverAliveCountMax;
            return this;
        }

        public Builder knownHostsMode(KnownHostsMode mode) {
            this.knownHostsMode = mode;
            return this;
        }

        public Builder knownHostsPath(String path) {
            this.knownHostsPath = path;
            return this;
        }

        public Builder authStrategy(AuthStrategy authStrategy) {
            this.authStrategy = authStrategy;
            return this;
        }

        public JschSessionFactory build() {
            Objects.requireNonNull(jschSupplier, "jschSupplier");
            Objects.requireNonNull(host, "host");
            Objects.requireNonNull(username, "username");
            return new JschSessionFactory(this);
        }
    }
}
