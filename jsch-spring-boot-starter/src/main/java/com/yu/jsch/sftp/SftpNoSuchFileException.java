package com.yu.jsch.sftp;

/**
 * Missing file or directory.
 */
public class SftpNoSuchFileException extends SftpClientException {
    public SftpNoSuchFileException(String message) {
        super(message);
    }

    public SftpNoSuchFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
