package com.yu.jsch.exec;

import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Result of executing a command over SSH.
 */
public final class ExecResult {

    private final String stdout;
    private final String stderr;
    private final int exitCode;
    private final boolean timedOut;
    private final Instant startedAt;
    private final Instant finishedAt;
    private final String command;
    private final Map<String, String> environment;
    private final boolean pty;
    private final Charset charset;
    private final int attempts;

    private ExecResult(Builder b) {
        this.stdout = b.stdout;
        this.stderr = b.stderr;
        this.exitCode = b.exitCode;
        this.timedOut = b.timedOut;
        this.startedAt = b.startedAt;
        this.finishedAt = b.finishedAt;
        this.command = b.command;
        this.environment = b.environment;
        this.pty = b.pty;
        this.charset = b.charset;
        this.attempts = b.attempts;
    }

    public String getStdout() {
        return stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public int getExitCode() {
        return exitCode;
    }

    public boolean isTimedOut() {
        return timedOut;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public Duration getDuration() {
        return (startedAt != null && finishedAt != null) ? Duration.between(startedAt, finishedAt) : null;
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

    public Charset getCharset() {
        return charset;
    }

    public int getAttempts() {
        return attempts;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String stdout = "";
        private String stderr = "";
        private int exitCode = -1;
        private boolean timedOut = false;
        private Instant startedAt = null;
        private Instant finishedAt = null;
        private String command;
        private Map<String, String> environment;
        private boolean pty;
        private Charset charset;
        private int attempts = 1;

        public Builder stdout(String stdout) {
            this.stdout = stdout;
            return this;
        }

        public Builder stderr(String stderr) {
            this.stderr = stderr;
            return this;
        }

        public Builder exitCode(int exitCode) {
            this.exitCode = exitCode;
            return this;
        }

        public Builder timedOut(boolean timedOut) {
            this.timedOut = timedOut;
            return this;
        }

        public Builder startedAt(Instant startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public Builder finishedAt(Instant finishedAt) {
            this.finishedAt = finishedAt;
            return this;
        }

        public Builder command(String command) {
            this.command = command;
            return this;
        }

        public Builder environment(Map<String, String> environment) {
            this.environment = environment;
            return this;
        }

        public Builder pty(boolean pty) {
            this.pty = pty;
            return this;
        }

        public Builder charset(Charset charset) {
            this.charset = charset;
            return this;
        }

        public Builder attempts(int attempts) {
            this.attempts = attempts;
            return this;
        }

        public ExecResult build() {
            return new ExecResult(this);
        }
    }
}
