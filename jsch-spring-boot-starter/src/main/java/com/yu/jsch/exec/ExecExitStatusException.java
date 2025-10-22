package com.yu.jsch.exec;

/**
 * Exception representing a non-zero exit status from a remote command.
 */
public class ExecExitStatusException extends Exception {
    private final int exitStatus;

    public ExecExitStatusException(String message, int exitStatus) {
        super(message);
        this.exitStatus = exitStatus;
    }

    public int getExitStatus() {
        return exitStatus;
    }
}
