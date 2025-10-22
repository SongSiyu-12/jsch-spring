package com.yu.jsch;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.yu.jsch.channel.SftpChannelFactory;
import com.yu.jsch.host.HostConfig;
import com.yu.jsch.observability.ObservabilityConfig;
import com.yu.jsch.sftp.*;
import com.yu.jsch.strategy.NoRetryStrategy;
import com.yu.jsch.strategy.RetryStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Vector;

/**
 * Template to perform SFTP operations managing session/channel lifecycle and retries.
 */
public class SftpTemplate {

    private static final Logger log = LoggerFactory.getLogger(SftpTemplate.class);

    private final JschSessionManager jschSessionManager;
    private final SftpChannelFactory sftpChannelFactory;
    private final RetryStrategy retryStrategy;
    private final ObservabilityConfig observability;
    private final String hostAlias;

    public SftpTemplate(JschSessionManager jschSessionManager) {
        this(jschSessionManager, new SftpChannelFactory(), new NoRetryStrategy(), ObservabilityConfig.disabled(), null);
    }

    public SftpTemplate(JschSessionManager jschSessionManager, SftpChannelFactory sftpChannelFactory, RetryStrategy retryStrategy) {
        this(jschSessionManager, sftpChannelFactory, retryStrategy, ObservabilityConfig.disabled(), null);
    }

    public SftpTemplate(JschSessionManager jschSessionManager, SftpChannelFactory sftpChannelFactory, RetryStrategy retryStrategy,
                        ObservabilityConfig observability, String hostAlias) {
        this.jschSessionManager = Objects.requireNonNull(jschSessionManager, "sessionManager");
        this.sftpChannelFactory = Objects.requireNonNull(sftpChannelFactory, "sftpChannelFactory");
        this.retryStrategy = Objects.requireNonNullElseGet(retryStrategy, NoRetryStrategy::new);
        this.observability = observability != null ? observability : ObservabilityConfig.disabled();
        this.hostAlias = hostAlias;
    }

    public void mkdir(String path, int connectTimeoutMillis) throws SftpClientException {
        execute("mkdir", idempotent(true), connectTimeoutMillis, sftp -> {
            sftp.mkdir(path);
            return null;
        });
    }

    public void delete(String path, int connectTimeoutMillis) throws SftpClientException {
        execute("delete", idempotent(true), connectTimeoutMillis, sftp -> {
            sftp.rm(path);
            return null;
        });
    }

    public void rename(String from, String to, boolean overwrite, int connectTimeoutMillis) throws SftpClientException {
        execute("rename", idempotent(true), connectTimeoutMillis, sftp -> {
            try {
                sftp.rename(from, to);
            } catch (SftpException ex) {
                if (overwrite) {
                    try {
                        sftp.rm(to);
                    } catch (SftpException ignore) {
                        // ignore
                    }
                    sftp.rename(from, to);
                } else {
                    throw ex;
                }
            }
            return null;
        });
    }

    public List<SftpFileInfo> list(String path, int connectTimeoutMillis) throws SftpClientException {
        return execute("list", idempotent(true), connectTimeoutMillis, sftp -> {
            Vector<ChannelSftp.LsEntry> entries = sftp.ls(path);
            List<SftpFileInfo> list = new ArrayList<>();
            for (ChannelSftp.LsEntry e : entries) {
                String name = e.getFilename();
                if (".".equals(name) || "..".equals(name)) continue;
                SftpATTRS a = e.getAttrs();
                boolean dir = a != null && a.isDir();
                long size = a != null ? a.getSize() : 0L;
                Instant mtime = a != null ? Instant.ofEpochSecond(a.getMTime()) : null;
                list.add(new SftpFileInfo(name, dir, size, mtime));
            }
            return list;
        });
    }

    public void upload(byte[] data, String remotePath, TransferOptions options) throws SftpClientException {
        Objects.requireNonNull(data, "data");
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
            upload(bais, remotePath, options);
        } catch (IOException ioe) {
            // ByteArrayInputStream does not throw, but keep for completeness
            throw new SftpIOException(ioe);
        }
    }

    public void upload(InputStream in, String remotePath, TransferOptions options) throws SftpClientException {
        Objects.requireNonNull(in, "in");
        Objects.requireNonNull(remotePath, "remotePath");
        TransferOptions opts = options != null ? options : TransferOptions.defaults();
        boolean idempotent = (in instanceof ByteArrayInputStream); // can retry safely
        execute("upload", idempotent(idempotent), opts.getConnectTimeoutMillis(), sftp -> {
            if (opts.isAtomic()) {
                String tmp = deriveTempPath(remotePath);
                sftp.put(in, tmp, ChannelSftp.OVERWRITE);
                performRenameWithOverwrite(sftp, tmp, remotePath, opts.isOverwrite());
                if (opts.getPermissions() != null) {
                    safeChmod(sftp, opts.getPermissions(), remotePath);
                }
            } else {
                if (!opts.isOverwrite()) {
                    // Fail if exists
                    try {
                        SftpATTRS attrs = sftp.stat(remotePath);
                        if (attrs != null) {
                            throw new SftpFileAlreadyExistsException("Remote file already exists: " + remotePath);
                        }
                    } catch (SftpException e) {
                        if (e.id != ChannelSftp.SSH_FX_NO_SUCH_FILE) throw e;
                    }
                }
                sftp.put(in, remotePath, ChannelSftp.OVERWRITE);
                if (opts.getPermissions() != null) {
                    safeChmod(sftp, opts.getPermissions(), remotePath);
                }
            }
            return null;
        });
    }

    public byte[] download(String remotePath, int connectTimeoutMillis) throws SftpClientException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        download(remotePath, baos, connectTimeoutMillis);
        return baos.toByteArray();
    }

    public void download(String remotePath, OutputStream out, int connectTimeoutMillis) throws SftpClientException {
        Objects.requireNonNull(out, "out");
        execute("download", idempotent(true), connectTimeoutMillis, sftp -> {
            sftp.get(remotePath, out);
            return null;
        });
    }

    // ---- HostConfig-aware variants ----

    public void mkdir(HostConfig hostConfig, String path, int connectTimeoutMillis) throws SftpClientException {
        execute(hostConfig, "mkdir", idempotent(true), connectTimeoutMillis, sftp -> {
            sftp.mkdir(path);
            return null;
        });
    }

    public void delete(HostConfig hostConfig, String path, int connectTimeoutMillis) throws SftpClientException {
        execute(hostConfig, "delete", idempotent(true), connectTimeoutMillis, sftp -> {
            sftp.rm(path);
            return null;
        });
    }

    public void rename(HostConfig hostConfig, String from, String to, boolean overwrite, int connectTimeoutMillis) throws SftpClientException {
        execute(hostConfig, "rename", idempotent(true), connectTimeoutMillis, sftp -> {
            try {
                sftp.rename(from, to);
            } catch (SftpException ex) {
                if (overwrite) {
                    try {
                        sftp.rm(to);
                    } catch (SftpException ignore) {
                    }
                    sftp.rename(from, to);
                } else {
                    throw ex;
                }
            }
            return null;
        });
    }

    public List<SftpFileInfo> list(HostConfig hostConfig, String path, int connectTimeoutMillis) throws SftpClientException {
        return execute(hostConfig, "list", idempotent(true), connectTimeoutMillis, sftp -> {
            Vector<ChannelSftp.LsEntry> entries = sftp.ls(path);
            List<SftpFileInfo> list = new ArrayList<>();
            for (ChannelSftp.LsEntry e : entries) {
                String name = e.getFilename();
                if (".".equals(name) || "..".equals(name)) continue;
                SftpATTRS a = e.getAttrs();
                boolean dir = a != null && a.isDir();
                long size = a != null ? a.getSize() : 0L;
                Instant mtime = a != null ? Instant.ofEpochSecond(a.getMTime()) : null;
                list.add(new SftpFileInfo(name, dir, size, mtime));
            }
            return list;
        });
    }

    public void upload(HostConfig hostConfig, byte[] data, String remotePath, TransferOptions options) throws SftpClientException {
        Objects.requireNonNull(data, "data");
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
            upload(hostConfig, bais, remotePath, options);
        } catch (IOException ioe) {
            throw new SftpIOException(ioe);
        }
    }

    public void upload(HostConfig hostConfig, InputStream in, String remotePath, TransferOptions options) throws SftpClientException {
        Objects.requireNonNull(in, "in");
        Objects.requireNonNull(remotePath, "remotePath");
        TransferOptions opts = options != null ? options : TransferOptions.defaults();
        boolean idempotent = (in instanceof ByteArrayInputStream);
        execute(hostConfig, "upload", idempotent(idempotent), opts.getConnectTimeoutMillis(), sftp -> {
            if (opts.isAtomic()) {
                String tmp = deriveTempPath(remotePath);
                sftp.put(in, tmp, ChannelSftp.OVERWRITE);
                performRenameWithOverwrite(sftp, tmp, remotePath, opts.isOverwrite());
                if (opts.getPermissions() != null) {
                    safeChmod(sftp, opts.getPermissions(), remotePath);
                }
            } else {
                if (!opts.isOverwrite()) {
                    try {
                        SftpATTRS attrs = sftp.stat(remotePath);
                        if (attrs != null) {
                            throw new SftpFileAlreadyExistsException("Remote file already exists: " + remotePath);
                        }
                    } catch (SftpException e) {
                        if (e.id != ChannelSftp.SSH_FX_NO_SUCH_FILE) throw e;
                    }
                }
                sftp.put(in, remotePath, ChannelSftp.OVERWRITE);
                if (opts.getPermissions() != null) {
                    safeChmod(sftp, opts.getPermissions(), remotePath);
                }
            }
            return null;
        });
    }

    public byte[] download(HostConfig hostConfig, String remotePath, int connectTimeoutMillis) throws SftpClientException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        download(hostConfig, remotePath, baos, connectTimeoutMillis);
        return baos.toByteArray();
    }

    public void download(HostConfig hostConfig, String remotePath, OutputStream out, int connectTimeoutMillis) throws SftpClientException {
        Objects.requireNonNull(out, "out");
        execute(hostConfig, "download", idempotent(true), connectTimeoutMillis, sftp -> {
            sftp.get(remotePath, out);
            return null;
        });
    }

    // --------------- internals ---------------

    private interface SftpCallback<T> {
        T doInSftp(ChannelSftp sftp) throws Exception;
    }

    private record ExecConfig(boolean idempotent) {
    }

    private ExecConfig idempotent(boolean v) {
        return new ExecConfig(v);
    }

    private <T> T execute(String op, ExecConfig cfg, int connectTimeoutMillis, SftpCallback<T> callback) throws SftpClientException {
        Objects.requireNonNull(callback, "callback");
        int attempt = 0;
        while (true) {
            attempt++;
            Instant start = Instant.now();
            if (observability.isLoggingEnabled()) {
                log.atInfo()
                        .addKeyValue("event", "start")
                        .addKeyValue("metric", observability.sftpOperationMetric())
                        .addKeyValue("alias", hostAlias)
                        .addKeyValue("op", op)
                        .addKeyValue("attempt", attempt)
                        .addKeyValue("idempotent", cfg.idempotent)
                        .addKeyValue("connect_timeout_ms", connectTimeoutMillis)
                        .log("sftp op");
            }
            try {
                T result = jschSessionManager.execute(session -> {
                    ChannelSftp sftp = null;
                    try {
                        sftp = sftpChannelFactory.open(session, connectTimeoutMillis);
                        return callback.doInSftp(sftp);
                    } finally {
                        if (sftp != null) {
                            try {
                                sftp.disconnect();
                            } catch (Throwable ignore) {
                            }
                        }
                    }
                });
                if (observability.isLoggingEnabled()) {
                    long durationMs = Duration.between(start, Instant.now()).toMillis();
                    log.atInfo()
                            .addKeyValue("event", "finish")
                            .addKeyValue("metric", observability.sftpOperationMetric())
                            .addKeyValue("alias", hostAlias)
                            .addKeyValue("op", op)
                            .addKeyValue("attempt", attempt)
                            .addKeyValue("duration_ms", durationMs)
                            .log("sftp op finished");
                }
                return result;
            } catch (Throwable ex) {
                boolean willRetry = cfg.idempotent && retryStrategy.shouldRetry(attempt, ex);
                if (observability.isLoggingEnabled()) {
                    long durationMs = Duration.between(start, Instant.now()).toMillis();
                    log.atWarn()
                            .addKeyValue("event", "failure")
                            .addKeyValue("metric", observability.sftpOperationMetric())
                            .addKeyValue("alias", hostAlias)
                            .addKeyValue("op", op)
                            .addKeyValue("attempt", attempt)
                            .addKeyValue("duration_ms", durationMs)
                            .addKeyValue("error", ex.getClass().getSimpleName())
                            .addKeyValue("message", ex.getMessage())
                            .addKeyValue("retrying", willRetry)
                            .setCause(ex)
                            .log("sftp op failed");
                }
                if (willRetry) {
                    sleepBeforeRetry(attempt);
                    continue;
                }
                throw mapException(ex);
            }
        }
    }

    private <T> T execute(HostConfig hostConfig, String op, ExecConfig cfg, int connectTimeoutMillis, SftpCallback<T> callback) throws SftpClientException {
        Objects.requireNonNull(hostConfig, "hostConfig");
        Objects.requireNonNull(callback, "callback");
        int attempt = 0;
        while (true) {
            attempt++;
            Instant start = Instant.now();
            if (observability.isLoggingEnabled()) {
                log.atInfo()
                        .addKeyValue("event", "start")
                        .addKeyValue("metric", observability.sftpOperationMetric())
                        .addKeyValue("alias", hostAlias)
                        .addKeyValue("op", op)
                        .addKeyValue("attempt", attempt)
                        .addKeyValue("idempotent", cfg.idempotent)
                        .addKeyValue("connect_timeout_ms", connectTimeoutMillis)
                        .log("sftp op");
            }
            try {
                T result = jschSessionManager.execute(hostConfig, session -> {
                    ChannelSftp sftp = null;
                    try {
                        sftp = sftpChannelFactory.open(session, connectTimeoutMillis);
                        return callback.doInSftp(sftp);
                    } finally {
                        if (sftp != null) {
                            try {
                                sftp.disconnect();
                            } catch (Throwable ignore) {
                            }
                        }
                    }
                });
                if (observability.isLoggingEnabled()) {
                    long durationMs = Duration.between(start, Instant.now()).toMillis();
                    log.atInfo()
                            .addKeyValue("event", "finish")
                            .addKeyValue("metric", observability.sftpOperationMetric())
                            .addKeyValue("alias", hostAlias)
                            .addKeyValue("op", op)
                            .addKeyValue("attempt", attempt)
                            .addKeyValue("duration_ms", durationMs)
                            .log("sftp op finished");
                }
                return result;
            } catch (Throwable ex) {
                boolean willRetry = cfg.idempotent && retryStrategy.shouldRetry(attempt, ex);
                if (observability.isLoggingEnabled()) {
                    long durationMs = Duration.between(start, Instant.now()).toMillis();
                    log.atWarn()
                            .addKeyValue("event", "failure")
                            .addKeyValue("metric", observability.sftpOperationMetric())
                            .addKeyValue("alias", hostAlias)
                            .addKeyValue("op", op)
                            .addKeyValue("attempt", attempt)
                            .addKeyValue("duration_ms", durationMs)
                            .addKeyValue("error", ex.getClass().getSimpleName())
                            .addKeyValue("message", ex.getMessage())
                            .addKeyValue("retrying", willRetry)
                            .setCause(ex)
                            .log("sftp op failed");
                }
                if (willRetry) {
                    sleepBeforeRetry(attempt);
                    continue;
                }
                throw mapException(ex);
            }
        }
    }

    private void performRenameWithOverwrite(ChannelSftp sftp, String from, String to, boolean overwrite) throws SftpException {
        try {
            sftp.rename(from, to);
        } catch (SftpException ex) {
            if (overwrite) {
                try {
                    sftp.rm(to);
                } catch (SftpException ignore) {
                }
                sftp.rename(from, to);
            } else {
                throw ex;
            }
        }
    }

    private void safeChmod(ChannelSftp sftp, int permissions, String path) throws SftpException {
        try {
            sftp.chmod(permissions, path);
        } catch (SftpException e) {
            // Some servers may not allow chmod; rethrow as permission denied
            throw e;
        }
    }

    private String deriveTempPath(String finalPath) {
        int slash = finalPath.lastIndexOf('/') + 1;
        String dir = slash > 0 ? finalPath.substring(0, slash) : "";
        String name = slash > 0 ? finalPath.substring(slash) : finalPath;
        String tmpName = ".tmp-" + name + "." + Long.toUnsignedString(System.nanoTime());
        return dir + tmpName;
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            long delay = retryStrategy.getDelayMillis(attempt);
            if (delay > 0) Thread.sleep(delay);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    // Visible for tests
    public static SftpClientException mapException(Throwable ex) throws SftpClientException {
        if (ex instanceof SftpClientException sce) return sce;
        if (ex instanceof SftpException sftpe) {
            return mapSftpException(sftpe);
        }
        if (ex instanceof JSchException jse) {
            String msg = jse.getMessage() != null ? jse.getMessage() : "JSch error";
            String lowerMsg = msg.toLowerCase();
            if (lowerMsg.contains("auth") && lowerMsg.contains("fail")) {
                return new SftpAuthenticationException("Authentication failed: " + msg, jse);
            }
            if (lowerMsg.contains("timeout") || lowerMsg.contains("connection") || lowerMsg.contains("connect")) {
                return new SftpConnectionException("Connection failed: " + msg, jse);
            }
            return new SftpConnectionException(msg, jse);
        }
        if (ex instanceof IOException ioe) {
            return new SftpIOException("I/O error: " + ioe.getMessage(), ioe);
        }
        if (ex instanceof InterruptedException ie) {
            Thread.currentThread().interrupt(); // Restore interrupted status
            return new SftpClientException("Operation was interrupted", ie);
        }
        if (ex instanceof Exception e) {
            return new SftpClientException("SFTP operation failed: " + e.getMessage(), e);
        }
        return new SftpClientException("Unexpected error: " + ex.getMessage(), ex);
    }

    private static SftpClientException mapSftpException(SftpException ex) {
        int id = ex.id;
        String msg = ex.getMessage();
        return switch (id) {
            case ChannelSftp.SSH_FX_NO_SUCH_FILE -> new SftpNoSuchFileException(msg, ex);
            case ChannelSftp.SSH_FX_PERMISSION_DENIED -> new SftpPermissionDeniedException(msg, ex);
            case ChannelSftp.SSH_FX_FAILURE -> new SftpOperationFailedException(msg, ex);
            case ChannelSftp.SSH_FX_NO_CONNECTION, ChannelSftp.SSH_FX_CONNECTION_LOST ->
                    new SftpConnectionException(msg, ex);
            case ChannelSftp.SSH_FX_OP_UNSUPPORTED -> new SftpOperationUnsupportedException(msg, ex);
            default -> new SftpClientException(msg, ex);
        };
    }
}
