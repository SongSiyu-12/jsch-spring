package com.yu.jsch;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.yu.jsch.channel.ExecChannelFactory;
import com.yu.jsch.exec.ExecExitStatusException;
import com.yu.jsch.exec.ExecResult;
import com.yu.jsch.exec.SshCommandRequest;
import com.yu.jsch.host.HostConfig;
import com.yu.jsch.observability.ObservabilityConfig;
import com.yu.jsch.strategy.NoRetryStrategy;
import com.yu.jsch.strategy.RetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Template for executing commands over SSH using a SessionManager and ExecChannelFactory.
 * Implements the Template Method pattern with overridable hooks.
 */
public class SshTemplate {

    private static final Logger log = LoggerFactory.getLogger(SshTemplate.class);

    private final JschSessionManager jschSessionManager;
    private final ExecChannelFactory execChannelFactory;
    private final RetryStrategy retryStrategy;
    private final ObservabilityConfig observability;
    private final String hostAlias;

    public SshTemplate(JschSessionManager jschSessionManager) {
        this(jschSessionManager, new ExecChannelFactory(), new NoRetryStrategy(), ObservabilityConfig.disabled(), null);
    }

    public SshTemplate(JschSessionManager jschSessionManager, ExecChannelFactory execChannelFactory, RetryStrategy retryStrategy) {
        this(jschSessionManager, execChannelFactory, retryStrategy, ObservabilityConfig.disabled(), null);
    }

    public SshTemplate(JschSessionManager jschSessionManager, ExecChannelFactory execChannelFactory, RetryStrategy retryStrategy,
                       ObservabilityConfig observability, String hostAlias) {
        this.jschSessionManager = Objects.requireNonNull(jschSessionManager, "sessionManager");
        this.execChannelFactory = Objects.requireNonNull(execChannelFactory, "execChannelFactory");
        this.retryStrategy = Objects.requireNonNullElseGet(retryStrategy, NoRetryStrategy::new);
        this.observability = observability != null ? observability : ObservabilityConfig.disabled();
        this.hostAlias = hostAlias;
    }

    /**
     * Execute a command using the provided request configuration.
     */
    public ExecResult execute(SshCommandRequest request) throws Exception {
        Objects.requireNonNull(request, "request");

        int attempt = 0;
        while (true) {
            attempt++;
            Instant start = Instant.now();
            if (observability.isLoggingEnabled()) {
                log.atInfo()
                        .addKeyValue("event", "start")
                        .addKeyValue("metric", observability.sshExecMetric())
                        .addKeyValue("alias", hostAlias)
                        .addKeyValue("command", request.getCommand())
                        .addKeyValue("attempt", attempt)
                        .addKeyValue("idempotent", request.isIdempotent())
                        .addKeyValue("connect_timeout_ms", request.getConnectTimeoutMillis())
                        .addKeyValue("exec_timeout", request.getExecutionTimeout())
                        .log("ssh exec");
            }
            try {
                int finalAttempt = attempt;
                ExecResult res = jschSessionManager.execute(session -> doExecuteInSession(session, request, finalAttempt));
                if (shouldRetryOnResult(request, res, attempt)) {
                    if (observability.isLoggingEnabled()) {
                        long durationMs = Duration.between(start, Instant.now()).toMillis();
                        log.atWarn()
                                .addKeyValue("event", "failure")
                                .addKeyValue("metric", observability.sshExecMetric())
                                .addKeyValue("alias", hostAlias)
                                .addKeyValue("command", request.getCommand())
                                .addKeyValue("attempt", attempt)
                                .addKeyValue("exit_code", res.getExitCode())
                                .addKeyValue("duration_ms", durationMs)
                                .addKeyValue("retrying", true)
                                .log("ssh exec non-zero exit");
                    }
                    sleepBeforeRetry(attempt);
                    continue;
                }
                if (observability.isLoggingEnabled()) {
                    long durationMs = Duration.between(start, Instant.now()).toMillis();
                    log.atInfo()
                            .addKeyValue("event", "finish")
                            .addKeyValue("metric", observability.sshExecMetric())
                            .addKeyValue("alias", hostAlias)
                            .addKeyValue("command", request.getCommand())
                            .addKeyValue("attempt", attempt)
                            .addKeyValue("exit_code", res.getExitCode())
                            .addKeyValue("duration_ms", durationMs)
                            .log("ssh exec finished");
                }
                return res;
            } catch (Throwable ex) {
                boolean willRetry = request.isIdempotent() && retryStrategy.shouldRetry(attempt, ex);
                if (observability.isLoggingEnabled()) {
                    long durationMs = Duration.between(start, Instant.now()).toMillis();
                    log.atWarn()
                            .addKeyValue("event", "failure")
                            .addKeyValue("metric", observability.sshExecMetric())
                            .addKeyValue("alias", hostAlias)
                            .addKeyValue("command", request.getCommand())
                            .addKeyValue("attempt", attempt)
                            .addKeyValue("duration_ms", durationMs)
                            .addKeyValue("error", ex.getClass().getSimpleName())
                            .addKeyValue("message", ex.getMessage())
                            .addKeyValue("retrying", willRetry)
                            .setCause(ex)
                            .log("ssh exec failed");
                }
                if (willRetry) {
                    sleepBeforeRetry(attempt);
                    continue;
                }
                // Rethrow preserving type
                if (ex instanceof Exception e) {
                    throw e;
                }
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * Execute a command using the provided request and HostConfig, routed through a HostConfig-aware SessionManager.
     */
    public ExecResult execute(HostConfig hostConfig, SshCommandRequest request) throws Exception {
        Objects.requireNonNull(hostConfig, "hostConfig");
        Objects.requireNonNull(request, "request");

        int attempt = 0;
        while (true) {
            attempt++;
            Instant start = Instant.now();
            if (observability.isLoggingEnabled()) {
                log.atInfo()
                        .addKeyValue("event", "start")
                        .addKeyValue("metric", observability.sshExecMetric())
                        .addKeyValue("alias", hostAlias)
                        .addKeyValue("command", request.getCommand())
                        .addKeyValue("attempt", attempt)
                        .addKeyValue("idempotent", request.isIdempotent())
                        .addKeyValue("connect_timeout_ms", request.getConnectTimeoutMillis())
                        .addKeyValue("exec_timeout", request.getExecutionTimeout())
                        .log("ssh exec");
            }
            try {
                int finalAttempt = attempt;
                ExecResult res = jschSessionManager.execute(hostConfig, session -> doExecuteInSession(session, request, finalAttempt));
                if (shouldRetryOnResult(request, res, attempt)) {
                    if (observability.isLoggingEnabled()) {
                        long durationMs = Duration.between(start, Instant.now()).toMillis();
                        log.atWarn()
                                .addKeyValue("event", "failure")
                                .addKeyValue("metric", observability.sshExecMetric())
                                .addKeyValue("alias", hostAlias)
                                .addKeyValue("command", request.getCommand())
                                .addKeyValue("attempt", attempt)
                                .addKeyValue("exit_code", res.getExitCode())
                                .addKeyValue("duration_ms", durationMs)
                                .addKeyValue("retrying", true)
                                .log("ssh exec non-zero exit");
                    }
                    sleepBeforeRetry(attempt);
                    continue;
                }
                if (observability.isLoggingEnabled()) {
                    long durationMs = Duration.between(start, Instant.now()).toMillis();
                    log.atInfo()
                            .addKeyValue("event", "finish")
                            .addKeyValue("metric", observability.sshExecMetric())
                            .addKeyValue("alias", hostAlias)
                            .addKeyValue("command", request.getCommand())
                            .addKeyValue("attempt", attempt)
                            .addKeyValue("exit_code", res.getExitCode())
                            .addKeyValue("duration_ms", durationMs)
                            .log("ssh exec finished");
                }
                return res;
            } catch (Throwable ex) {
                boolean willRetry = request.isIdempotent() && retryStrategy.shouldRetry(attempt, ex);
                if (observability.isLoggingEnabled()) {
                    long durationMs = Duration.between(start, Instant.now()).toMillis();
                    log.atWarn()
                            .addKeyValue("event", "failure")
                            .addKeyValue("metric", observability.sshExecMetric())
                            .addKeyValue("alias", hostAlias)
                            .addKeyValue("command", request.getCommand())
                            .addKeyValue("attempt", attempt)
                            .addKeyValue("duration_ms", durationMs)
                            .addKeyValue("error", ex.getClass().getSimpleName())
                            .addKeyValue("message", ex.getMessage())
                            .addKeyValue("retrying", willRetry)
                            .setCause(ex)
                            .log("ssh exec failed");
                }
                if (willRetry) {
                    sleepBeforeRetry(attempt);
                    continue;
                }
                if (ex instanceof Exception e) {
                    throw e;
                }
                throw new RuntimeException(ex);
            }
        }
    }

    protected ExecResult doExecuteInSession(Session session, SshCommandRequest request, int attempt) throws Exception {
        ChannelExec channel = null;
        Instant start = Instant.now();
        try {
            channel = openExecChannel(session);
            configureChannel(channel, request);
            connectChannel(channel, request.getConnectTimeoutMillis());

            ExecStreams streams = openStreams(channel);
            boolean timedOut = waitForCompletion(channel, request.getExecutionTimeout());

            // Ensure channel is closed to get exit status
            int exitCode = channel.getExitStatus();

            // Complete stream reading
            String stdout = readFully(streams.stdout, request.getCharset());
            String stderr = readFully(streams.stderr, request.getCharset());

            return ExecResult.builder()
                    .command(request.getCommand())
                    .environment(request.getEnvironment())
                    .pty(request.isPty())
                    .charset(request.getCharset())
                    .startedAt(start)
                    .finishedAt(Instant.now())
                    .stdout(stdout)
                    .stderr(stderr)
                    .exitCode(exitCode)
                    .timedOut(timedOut)
                    .attempts(attempt)
                    .build();
        } finally {
            cleanup(channel, session);
        }
    }

    protected ChannelExec openExecChannel(Session session) throws JSchException {
        return execChannelFactory.open(session);
    }

    protected void configureChannel(ChannelExec channel, SshCommandRequest request) {
        channel.setCommand(request.getCommand());
        if (request.isPty()) {
            channel.setPty(true);
            if (request.getPtyType() != null && !request.getPtyType().isBlank()) {
                channel.setPtyType(request.getPtyType());
            }
        }
        Map<String, String> env = request.getEnvironment();
        if (env != null && !env.isEmpty()) {
            for (Map.Entry<String, String> e : env.entrySet()) {
                channel.setEnv(e.getKey(), e.getValue());
            }
        }
    }

    protected void connectChannel(ChannelExec channel, int connectTimeoutMillis) throws JSchException {
        if (connectTimeoutMillis > 0) channel.connect(connectTimeoutMillis);
        else channel.connect();
    }

    protected ExecStreams openStreams(ChannelExec channel) throws IOException {
        InputStream stdout = channel.getInputStream();
        InputStream stderr;
        try {
            stderr = channel.getExtInputStream();
        } catch (Exception ignore) {
            // Fallback to empty stream if extended stream unavailable
            stderr = InputStream.nullInputStream();
        }
        return new ExecStreams(stdout, stderr);
    }

    protected boolean waitForCompletion(ChannelExec channel, Duration timeout) throws InterruptedException {
        long deadline = timeout != null ? System.nanoTime() + timeout.toNanos() : Long.MAX_VALUE;
        while (true) {
            if (channel.isClosed()) return false;
            if (System.nanoTime() > deadline) {
                try {
                    channel.disconnect();
                } catch (Throwable ignore) {
                }
                return true;
            }
            Thread.sleep(50);
        }
    }

    protected String readFully(InputStream in, Charset charset) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int r;
        while ((r = in.read(buf)) != -1) {
            baos.write(buf, 0, r);
        }
        return baos.toString(charset);
    }

    protected void cleanup(ChannelExec channel, Session session) {
        if (channel != null) {
            try {
                channel.disconnect();
            } catch (Throwable ignore) {
            }
        }
        // Session is managed by SessionManager; no action here
    }

    private boolean shouldRetryOnResult(SshCommandRequest request, ExecResult res, int attempt) {
        if (!request.isIdempotent()) return false;
        if (res.getExitCode() == 0) return false;
        return retryStrategy.shouldRetry(attempt, new ExecExitStatusException("exit=" + res.getExitCode(), res.getExitCode()));
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            long delay = retryStrategy.getDelayMillis(attempt);
            if (delay > 0) Thread.sleep(delay);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private record ExecStreams(InputStream stdout, InputStream stderr) {
    }
}
