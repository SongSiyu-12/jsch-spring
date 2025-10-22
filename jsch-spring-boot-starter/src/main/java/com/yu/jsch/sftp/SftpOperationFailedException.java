package com.yu.jsch.sftp;

/**
 * Operation failed for unspecified reason reported by server.
 */
public class SftpOperationFailedException extends SftpClientException {
    public SftpOperationFailedException(String message) {
        super(message);
    }

    public SftpOperationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
