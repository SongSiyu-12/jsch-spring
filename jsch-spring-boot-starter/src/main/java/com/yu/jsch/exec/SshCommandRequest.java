package com.yu.jsch.exec;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable request configuration for executing a command over SSH.
 */
public final class SshCommandRequest {

    private final String command;
    private final Map<String, String> environment;
    private final boolean pty;
    private final String ptyType;
    private final Charset charset;

    /**
     * Timeout used when connecting the exec channel (0 or negative means default).
     */
    private final int connectTimeoutMillis;
    /**
     * Optional maximum duration for the command execution. Null means no explicit timeout.
     */
    private final Duration executionTimeout;

    /**
     * Whether the command is idempotent and can be retried if a retry strategy is configured.
     */
    private final boolean idempotent;

    private SshCommandRequest(Builder b) {
        this.command = Objects.requireNonNull(b.command, "command");
        this.environment = Collections.unmodifiableMap(new LinkedHashMap<>(b.environment));
        this.pty = b.pty;
        this.ptyType = b.ptyType;
        this.charset = b.charset;
        this.connectTimeoutMillis = b.connectTimeoutMillis;
        this.executionTimeout = b.executionTimeout;
        this.idempotent = b.idempotent;
    }

    public String getCommand() {
        return command;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public boolean isPty() {
        return pty;
    }

    public String getPtyType() {
        return ptyType;
    }

    public Charset getCharset() {
        return charset;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public Duration getExecutionTimeout() {
        return executionTimeout;
    }

    public boolean isIdempotent() {
        return idempotent;
    }

    public static Builder builder(String command) {
        return new Builder(command);
    }

    public static final class Builder {
        private final String command;
        private Map<String, String> environment = new LinkedHashMap<>();
        private boolean pty = false;
        private String ptyType = null;
        private Charset charset = StandardCharsets.UTF_8;
        private int connectTimeoutMillis = 0;
        private Duration executionTimeout = null;
        private boolean idempotent = true;

        public Builder(String command) {
            this.command = Objects.requireNonNull(command, "command");
        }

        public Builder env(String key, String value) {
            this.environment.put(Objects.requireNonNull(key), Objects.requireNonNullElse(value, ""));
            return this;
        }

        public Builder environment(Map<String, String> env) {
            if (env != null) this.environment.putAll(env);
            return this;
        }

        public Builder pty(boolean pty) {
            this.pty = pty;
            return this;
        }

        public Builder ptyType(String ptyType) {
            this.ptyType = ptyType;
            return this;
        }

        public Builder charset(Charset charset) {
            this.charset = Objects.requireNonNull(charset);
            return this;
        }

        public Builder connectTimeoutMillis(int connectTimeoutMillis) {
            this.connectTimeoutMillis = connectTimeoutMillis;
            return this;
        }

        public Builder executionTimeout(Duration executionTimeout) {
            this.executionTimeout = executionTimeout;
            return this;
        }

        public Builder idempotent(boolean idempotent) {
            this.idempotent = idempotent;
            return this;
        }

        public SshCommandRequest build() {
            return new SshCommandRequest(this);
        }
    }
}
