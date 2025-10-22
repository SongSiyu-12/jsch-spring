package com.yu.jsch.sftp;

/**
 * Operation is not supported by the SFTP server.
 */
public class SftpOperationUnsupportedException extends SftpClientException {
    public SftpOperationUnsupportedException(String message) {
        super(message);
    }

    public SftpOperationUnsupportedException(String message, Throwable cause) {
        super(message, cause);
    }
}
