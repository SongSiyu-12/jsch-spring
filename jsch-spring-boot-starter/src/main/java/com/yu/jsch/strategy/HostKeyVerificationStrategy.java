package com.yu.jsch.strategy;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.yu.jsch.HostDefinition;

/**
 * Strategy for configuring host key verification behavior on a {@link JSch} instance.
 */
public interface HostKeyVerificationStrategy {
    void apply(JSch jsch, HostDefinition host) throws JSchException;
}
