package com.yu.jsch.client;

import com.yu.jsch.SftpTemplate;
import com.yu.jsch.sftp.SftpClientException;
import com.yu.jsch.sftp.SftpFileInfo;
import com.yu.jsch.sftp.TransferOptions;

import java.io.InputStream;
import java.util.*;

/**
 * Default implementation of SftpClient delegating to SftpTemplate instances per host.
 */
public class DefaultSftpClient implements SftpClient {

    public static class HostContext {
        final SftpTemplate template;
        final int connectTimeoutMillis;

        public HostContext(SftpTemplate template, int connectTimeoutMillis) {
            this.template = Objects.requireNonNull(template, "template");
            this.connectTimeoutMillis = connectTimeoutMillis;
        }
    }

    private final Map<String, HostContext> hosts;
    private final String defaultHostAlias;

    public DefaultSftpClient(Map<String, HostContext> hosts, String defaultHostAlias) {
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
    public void mkdir(String path) throws SftpClientException {
        mkdir(resolveDefaultAlias(), path);
    }

    @Override
    public void mkdir(String hostAlias, String path) throws SftpClientException {
        HostContext ctx = hosts.get(hostAlias);
        if (ctx == null) throw new HostNotFoundException(hostAlias);
        ctx.template.mkdir(path, ctx.connectTimeoutMillis);
    }

    @Override
    public void delete(String path) throws SftpClientException {
        delete(resolveDefaultAlias(), path);
    }

    @Override
    public void delete(String hostAlias, String path) throws SftpClientException {
        HostContext ctx = hosts.get(hostAlias);
        if (ctx == null) throw new HostNotFoundException(hostAlias);
        ctx.template.delete(path, ctx.connectTimeoutMillis);
    }

    @Override
    public void rename(String from, String to, boolean overwrite) throws SftpClientException {
        rename(resolveDefaultAlias(), from, to, overwrite);
    }

    @Override
    public void rename(String hostAlias, String from, String to, boolean overwrite) throws SftpClientException {
        HostContext ctx = hosts.get(hostAlias);
        if (ctx == null) throw new HostNotFoundException(hostAlias);
        ctx.template.rename(from, to, overwrite, ctx.connectTimeoutMillis);
    }

    @Override
    public List<SftpFileInfo> list(String path) throws SftpClientException {
        return list(resolveDefaultAlias(), path);
    }

    @Override
    public List<SftpFileInfo> list(String hostAlias, String path) throws SftpClientException {
        HostContext ctx = hosts.get(hostAlias);
        if (ctx == null) throw new HostNotFoundException(hostAlias);
        return ctx.template.list(path, ctx.connectTimeoutMillis);
    }

    @Override
    public void upload(byte[] data, String remotePath, TransferOptions options) throws SftpClientException {
        upload(resolveDefaultAlias(), data, remotePath, options);
    }

    @Override
    public void upload(String hostAlias, byte[] data, String remotePath, TransferOptions options) throws SftpClientException {
        HostContext ctx = hosts.get(hostAlias);
        if (ctx == null) throw new HostNotFoundException(hostAlias);
        ctx.template.upload(data, remotePath, options);
    }

    @Override
    public void upload(InputStream in, String remotePath, TransferOptions options) throws SftpClientException {
        upload(resolveDefaultAlias(), in, remotePath, options);
    }

    @Override
    public void upload(String hostAlias, InputStream in, String remotePath, TransferOptions options) throws SftpClientException {
        HostContext ctx = hosts.get(hostAlias);
        if (ctx == null) throw new HostNotFoundException(hostAlias);
        ctx.template.upload(in, remotePath, options);
    }

    @Override
    public byte[] download(String remotePath) throws SftpClientException {
        return download(resolveDefaultAlias(), remotePath);
    }

    @Override
    public byte[] download(String hostAlias, String remotePath) throws SftpClientException {
        HostContext ctx = hosts.get(hostAlias);
        if (ctx == null) throw new HostNotFoundException(hostAlias);
        return ctx.template.download(remotePath, ctx.connectTimeoutMillis);
    }

    private String resolveDefaultAlias() {
        if (defaultHostAlias == null) {
            throw new IllegalStateException("Multiple hosts configured; specify host alias explicitly or set a default");
        }
        return defaultHostAlias;
    }
}
