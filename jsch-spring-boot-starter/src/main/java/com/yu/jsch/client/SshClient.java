package com.yu.jsch.client;

import com.yu.jsch.exec.ExecResult;
import com.yu.jsch.exec.SshCommandRequest;
import com.yu.jsch.exec.SshExecutionException;
import com.yu.jsch.host.HostConfig;

/**
 * High-level SSH client facade that handles host selection, retries, and exception translation.
 */
public interface SshClient {

    ExecResult exec(String command) throws SshExecutionException;

    ExecResult exec(String hostAlias, String command) throws SshExecutionException;

    ExecResult exec(SshCommandRequest request) throws SshExecutionException;

    ExecResult exec(String hostAlias, SshCommandRequest request) throws SshExecutionException;

    /**
     * Execute a command by providing HostConfig directly, bypassing host alias resolution.
     */
    default ExecResult exec(HostConfig hostConfig, String command) throws SshExecutionException {
        return exec(hostConfig, SshCommandRequest.builder(command).build());
    }

    /**
     * Execute with a HostConfig and request options.
     */
    default ExecResult exec(HostConfig hostConfig, SshCommandRequest request) throws SshExecutionException {
        throw new UnsupportedOperationException("Direct HostConfig execution not supported by this implementation");
    }
}
