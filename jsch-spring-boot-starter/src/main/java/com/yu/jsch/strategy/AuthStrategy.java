package com.yu.jsch.strategy;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.yu.jsch.HostDefinition;

/**
 * Strategy for configuring authentication on a JSCH {@link Session}.
 */
public interface AuthStrategy {
    /**
     * Configure authentication on the provided {@link Session} and/or {@link JSch} instance.
     *
     * @param jsch    JSCH instance which may be used for key-based auth setup
     * @param session session to configure
     * @param host    resolved host definition providing auth data
     * @throws JSchException if JSCH reports errors
     */
    void configure(JSch jsch, Session session, HostDefinition host) throws JSchException;
}
