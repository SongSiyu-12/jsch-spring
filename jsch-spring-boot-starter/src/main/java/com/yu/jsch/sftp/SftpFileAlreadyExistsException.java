package com.yu.jsch.sftp;

/**
 * Thrown when attempting to create a file that already exists.
 */
public class SftpFileAlreadyExistsException extends SftpClientException {
    public SftpFileAlreadyExistsException(String message) {
        super(message);
    }

    public SftpFileAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}
