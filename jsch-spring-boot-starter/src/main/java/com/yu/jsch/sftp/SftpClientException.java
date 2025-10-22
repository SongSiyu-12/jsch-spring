package com.yu.jsch.sftp;

/**
 * Base exception for SFTP client operations.
 */
public class SftpClientException extends Exception {
    public SftpClientException(String message) {
        super(message);
    }

    public SftpClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public SftpClientException(Throwable cause) {
        super(cause);
    }
}
