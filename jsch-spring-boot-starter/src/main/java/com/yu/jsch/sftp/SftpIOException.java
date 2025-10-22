package com.yu.jsch.sftp;

import java.io.IOException;

/**
 * IO error during local stream operations.
 */
public class SftpIOException extends SftpClientException {
    public SftpIOException(String message, IOException cause) {
        super(message, cause);
    }

    public SftpIOException(IOException cause) {
        super(cause);
    }
}
