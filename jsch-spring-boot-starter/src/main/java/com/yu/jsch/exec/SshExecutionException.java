package com.yu.jsch.exec;

/**
 * Top-level exception thrown by SshClient facade on execution failures.
 * Wraps lower-level exceptions such as JSchException, IOExceptions, or timeouts.
 */
public class SshExecutionException extends Exception {
    private final String hostAlias;
    private final String command;

    public SshExecutionException(String message) {
        this(message, null, null, null);
    }

    public SshExecutionException(String message, Throwable cause) {
        this(message, null, null, cause);
    }

    public SshExecutionException(String message, String hostAlias, String command, Throwable cause) {
        super(message, cause);
        this.hostAlias = hostAlias;
        this.command = command;
    }

    public String getHostAlias() {
        return hostAlias;
    }

    public String getCommand() {
        return command;
    }
}
