package com.yu.jsch.sftp;

/**
 * Permission denied by the server.
 */
public class SftpPermissionDeniedException extends SftpClientException {
    public SftpPermissionDeniedException(String message) {
        super(message);
    }

    public SftpPermissionDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}
