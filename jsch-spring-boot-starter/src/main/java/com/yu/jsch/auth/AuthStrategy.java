package com.yu.jsch.auth;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Strategy for applying authentication configuration to a JSch session.
 */
public interface AuthStrategy {
    /**
     * Configure authentication on the provided JSch instance and Session.
     * Implementations may add identities to JSch and/or set password or user info on the session.
     */
    void configure(JSch jsch, Session session) throws JSchException;
}
