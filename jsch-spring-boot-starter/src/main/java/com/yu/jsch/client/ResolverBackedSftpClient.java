package com.yu.jsch.client;

import com.yu.jsch.SftpTemplate;
import com.yu.jsch.host.HostConfig;
import com.yu.jsch.host.HostResolver;
import com.yu.jsch.sftp.SftpClientException;
import com.yu.jsch.sftp.SftpFileInfo;
import com.yu.jsch.sftp.TransferOptions;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

/**
 * SftpClient implementation that resolves HostConfig on each call via HostResolver,
 * and supports direct HostConfig uploads/downloads.
 */
public class ResolverBackedSftpClient implements SftpClient {

    private final HostResolver resolver;
    private final SftpTemplate template;
    private final String defaultHostAlias;

    public ResolverBackedSftpClient(HostResolver resolver, SftpTemplate template, String defaultHostAlias) {
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.template = Objects.requireNonNull(template, "template");
        this.defaultHostAlias = defaultHostAlias;
    }

    @Override
    public void mkdir(String path) throws SftpClientException {
        mkdir(resolveDefaultAlias(), path);
    }

    @Override
    public void mkdir(String hostAlias, String path) throws SftpClientException {
        HostConfig cfg = resolver.resolve(hostAlias).orElseThrow(() -> new HostNotFoundException(hostAlias));
        template.mkdir(cfg, path, cfg.getConnectTimeoutMillis());
    }

    @Override
    public void delete(String path) throws SftpClientException {
        delete(resolveDefaultAlias(), path);
    }

    @Override
    public void delete(String hostAlias, String path) throws SftpClientException {
        HostConfig cfg = resolver.resolve(hostAlias).orElseThrow(() -> new HostNotFoundException(hostAlias));
        template.delete(cfg, path, cfg.getConnectTimeoutMillis());
    }

    @Override
    public void rename(String from, String to, boolean overwrite) throws SftpClientException {
        rename(resolveDefaultAlias(), from, to, overwrite);
    }

    @Override
    public void rename(String hostAlias, String from, String to, boolean overwrite) throws SftpClientException {
        HostConfig cfg = resolver.resolve(hostAlias).orElseThrow(() -> new HostNotFoundException(hostAlias));
        template.rename(cfg, from, to, overwrite, cfg.getConnectTimeoutMillis());
    }

    @Override
    public List<SftpFileInfo> list(String path) throws SftpClientException {
        return list(resolveDefaultAlias(), path);
    }

    @Override
    public List<SftpFileInfo> list(String hostAlias, String path) throws SftpClientException {
        HostConfig cfg = resolver.resolve(hostAlias).orElseThrow(() -> new HostNotFoundException(hostAlias));
        return template.list(cfg, path, cfg.getConnectTimeoutMillis());
    }

    @Override
    public void upload(byte[] data, String remotePath, TransferOptions options) throws SftpClientException {
        upload(resolveDefaultAlias(), data, remotePath, options);
    }

    @Override
    public void upload(String hostAlias, byte[] data, String remotePath, TransferOptions options) throws SftpClientException {
        HostConfig cfg = resolver.resolve(hostAlias).orElseThrow(() -> new HostNotFoundException(hostAlias));
        template.upload(cfg, data, remotePath, options);
    }

    @Override
    public void upload(InputStream in, String remotePath, TransferOptions options) throws SftpClientException {
        upload(resolveDefaultAlias(), in, remotePath, options);
    }

    @Override
    public void upload(String hostAlias, InputStream in, String remotePath, TransferOptions options) throws SftpClientException {
        HostConfig cfg = resolver.resolve(hostAlias).orElseThrow(() -> new HostNotFoundException(hostAlias));
        template.upload(cfg, in, remotePath, options);
    }

    @Override
    public byte[] download(String remotePath) throws SftpClientException {
        return download(resolveDefaultAlias(), remotePath);
    }

    @Override
    public byte[] download(String hostAlias, String remotePath) throws SftpClientException {
        HostConfig cfg = resolver.resolve(hostAlias).orElseThrow(() -> new HostNotFoundException(hostAlias));
        return template.download(cfg, remotePath, cfg.getConnectTimeoutMillis());
    }

    @Override
    public void upload(HostConfig hostConfig, byte[] data, String remotePath, TransferOptions options) throws SftpClientException {
        Objects.requireNonNull(hostConfig, "hostConfig");
        template.upload(hostConfig, data, remotePath, options);
    }

    @Override
    public void upload(HostConfig hostConfig, InputStream in, String remotePath, TransferOptions options) throws SftpClientException {
        Objects.requireNonNull(hostConfig, "hostConfig");
        template.upload(hostConfig, in, remotePath, options);
    }

    @Override
    public byte[] download(HostConfig hostConfig, String remotePath) throws SftpClientException {
        Objects.requireNonNull(hostConfig, "hostConfig");
        return template.download(hostConfig, remotePath, hostConfig.getConnectTimeoutMillis());
    }

    @Override
    public List<SftpFileInfo> list(HostConfig hostConfig, String path) throws SftpClientException {
        Objects.requireNonNull(hostConfig, "hostConfig");
        return template.list(hostConfig, path, hostConfig.getConnectTimeoutMillis());
    }

    @Override
    public void delete(HostConfig hostConfig, String path) throws SftpClientException {
        Objects.requireNonNull(hostConfig, "hostConfig");
        template.delete(hostConfig, path, hostConfig.getConnectTimeoutMillis());
    }

    private String resolveDefaultAlias() {
        if (defaultHostAlias == null) {
            throw new IllegalStateException("Multiple hosts configured; specify host alias explicitly or set a default");
        }
        return defaultHostAlias;
    }
}
