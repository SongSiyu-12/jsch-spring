package com.yu.jsch.sftp;

/**
 * Connection or transport-level failure.
 */
public class SftpConnectionException extends SftpClientException {
    public SftpConnectionException(String message) {
        super(message);
    }

    public SftpConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
