package com.yu.jsch.sftp;

/**
 * Authentication failure while establishing SFTP session.
 */
public class SftpAuthenticationException extends SftpClientException {
    public SftpAuthenticationException(String message) {
        super(message);
    }

    public SftpAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
