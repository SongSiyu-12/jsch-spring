package com.yu.jsch.client;

import com.yu.jsch.host.HostConfig;
import com.yu.jsch.sftp.SftpClientException;
import com.yu.jsch.sftp.SftpFileInfo;
import com.yu.jsch.sftp.TransferOptions;

import java.io.InputStream;
import java.util.List;

/**
 * High-level SFTP client facade that handles host selection, retries, and exception translation.
 */
public interface SftpClient {

    void mkdir(String path) throws SftpClientException;

    void mkdir(String hostAlias, String path) throws SftpClientException;

    void delete(String path) throws SftpClientException;

    void delete(String hostAlias, String path) throws SftpClientException;

    void rename(String from, String to, boolean overwrite) throws SftpClientException;

    void rename(String hostAlias, String from, String to, boolean overwrite) throws SftpClientException;

    List<SftpFileInfo> list(String path) throws SftpClientException;

    List<SftpFileInfo> list(String hostAlias, String path) throws SftpClientException;

    void upload(byte[] data, String remotePath, TransferOptions options) throws SftpClientException;

    void upload(String hostAlias, byte[] data, String remotePath, TransferOptions options) throws SftpClientException;

    void upload(InputStream in, String remotePath, TransferOptions options) throws SftpClientException;

    void upload(String hostAlias, InputStream in, String remotePath, TransferOptions options) throws SftpClientException;

    byte[] download(String remotePath) throws SftpClientException;

    byte[] download(String hostAlias, String remotePath) throws SftpClientException;

    // HostConfig direct variants
    default void upload(HostConfig hostConfig, byte[] data, String remotePath, TransferOptions options) throws SftpClientException {
        throw new UnsupportedOperationException("Direct HostConfig upload not supported by this implementation");
    }

    default void upload(HostConfig hostConfig, InputStream in, String remotePath, TransferOptions options) throws SftpClientException {
        throw new UnsupportedOperationException("Direct HostConfig upload not supported by this implementation");
    }

    default byte[] download(HostConfig hostConfig, String remotePath) throws SftpClientException {
        throw new UnsupportedOperationException("Direct HostConfig download not supported by this implementation");
    }

    default List<SftpFileInfo> list(HostConfig hostConfig, String path) throws SftpClientException {
        throw new UnsupportedOperationException("Direct HostConfig list not supported by this implementation");
    }

    default void delete(HostConfig hostConfig, String path) throws SftpClientException {
        throw new UnsupportedOperationException("Direct HostConfig delete not supported by this implementation");
    }
}
