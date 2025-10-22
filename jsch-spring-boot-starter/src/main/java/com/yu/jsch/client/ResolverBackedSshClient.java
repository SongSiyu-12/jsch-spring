package com.yu.jsch.client;

import com.yu.jsch.SshTemplate;
import com.yu.jsch.exec.ExecResult;
import com.yu.jsch.exec.SshCommandRequest;
import com.yu.jsch.exec.SshExecutionException;
import com.yu.jsch.host.HostConfig;
import com.yu.jsch.host.HostResolver;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * SshClient implementation that resolves HostConfig via a HostResolver at call time,
 * and supports direct HostConfig execution as well.
 */
public class ResolverBackedSshClient implements SshClient {

    private final HostResolver hostResolver;
    private final SshTemplate template;
    private final String defaultHostAlias;

    public ResolverBackedSshClient(HostResolver hostResolver, SshTemplate template, String defaultHostAlias) {
        this.hostResolver = Objects.requireNonNull(hostResolver, "hostResolver");
        this.template = Objects.requireNonNull(template, "template");
        this.defaultHostAlias = defaultHostAlias;
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
        HostConfig cfg = hostResolver.resolve(hostAlias).orElseThrow(() -> new HostNotFoundException(hostAlias));
        SshCommandRequest reqToUse = ensureConnectTimeout(request, cfg.getConnectTimeoutMillis());
        try {
            return template.execute(cfg, reqToUse);
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "SSH execution failed";
            throw new SshExecutionException(msg, hostAlias, request.getCommand(), ex);
        }
    }

    @Override
    public ExecResult exec(HostConfig hostConfig, SshCommandRequest request) throws SshExecutionException {
        Objects.requireNonNull(hostConfig, "hostConfig");
        Objects.requireNonNull(request, "request");
        SshCommandRequest reqToUse = ensureConnectTimeout(request, hostConfig.getConnectTimeoutMillis());
        try {
            return template.execute(hostConfig, reqToUse);
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : "SSH execution failed";
            throw new SshExecutionException(msg, hostConfig.stableKey(), request.getCommand(), ex);
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
