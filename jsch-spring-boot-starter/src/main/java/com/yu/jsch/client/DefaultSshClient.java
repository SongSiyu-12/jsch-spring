package com.yu.jsch.client;

import com.yu.jsch.SshTemplate;
import com.yu.jsch.exec.ExecResult;
import com.yu.jsch.exec.SshCommandRequest;
import com.yu.jsch.exec.SshExecutionException;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Default implementation of SshClient delegating to SshTemplate instances per host.
 */
public class DefaultSshClient implements SshClient {

    public static class HostContext {
        final SshTemplate template;
        final int connectTimeoutMillis;

        public HostContext(SshTemplate template, int connectTimeoutMillis) {
            this.template = Objects.requireNonNull(template, "template");
            this.connectTimeoutMillis = connectTimeoutMillis;
        }
    }

    private final Map<String, HostContext> hosts;
    private final String defaultHostAlias;

    public DefaultSshClient(Map<String, HostContext> hosts, String defaultHostAlias) {
        if (hosts == null || hosts.isEmpty())
            throw new IllegalArgumentException("At least one host must be configured");
        this.hosts = Collections.unmodifiableMap(new HashMap<>(hosts));
        if (defaultHostAlias != null && !this.hosts.containsKey(defaultHostAlias)) {
            throw new IllegalArgumentException("Default host alias not found: " + defaultHostAlias);
        }
        if (defaultHostAlias == null && this.hosts.size() == 1) {
            this.defaultHostAlias = this.hosts.keySet().iterator().next();
        } else {
            this.defaultHostAlias = defaultHostAlias;
        }
    }

    @Override
    public ExecResult exec(String command) throws SshExecutionException {
        return exec(resolveDefaultAlias(), command);
    }

    @Override
    public ExecResult exec(String hostAlias, String command) throws SshExecutionException {
        Objects.requireNonNull(command, "command");
        SshCommandRequest request = SshCommandRequest.builder(command)
                .charset(StandardCharsets.UTF_8)
                .idempotent(true)
                .build();
        return exec(hostAlias, request);
    }

    @Override
    public ExecResult exec(SshCommandRequest request) throws SshExecutionException {
        return exec(resolveDefaultAlias(), request);
    }

    @Override
    public ExecResult exec(String hostAlias, SshCommandRequest request) throws SshExecutionException {
        Objects.requireNonNull(request, "request");
        HostContext ctx = hosts.get(hostAlias);
        if (ctx == null) throw new HostNotFoundException(hostAlias);
        SshCommandRequest reqToUse = ensureConnectTimeout(request, ctx.connectTimeoutMillis);
        try {
            return ctx.template.execute(reqToUse);
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "SSH execution failed";
            throw new SshExecutionException(msg, hostAlias, request.getCommand(), ex);
        }
    }

    private String resolveDefaultAlias() {
        if (defaultHostAlias == null) {
            throw new IllegalStateException("Multiple hosts configured; specify host alias explicitly or set a default");
        }
        return defaultHostAlias;
    }

    private static SshCommandRequest ensureConnectTimeout(SshCommandRequest request, int connectTimeout) {
        if (request.getConnectTimeoutMillis() > 0) return request;
        SshCommandRequest.Builder b = SshCommandRequest.builder(request.getCommand())
                .environment(request.getEnvironment())
                .pty(request.isPty())
                .ptyType(request.getPtyType())
                .charset(request.getCharset())
                .connectTimeoutMillis(connectTimeout)
                .executionTimeout(request.getExecutionTimeout())
                .idempotent(request.isIdempotent());
        return b.build();
    }
}
