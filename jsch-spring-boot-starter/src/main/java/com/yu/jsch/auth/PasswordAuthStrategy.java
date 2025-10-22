package com.yu.jsch.auth;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Password-based authentication.
 */
public class PasswordAuthStrategy implements AuthStrategy {
    private final String password;

    public PasswordAuthStrategy(String password) {
        this.password = password;
    }

    @Override
    public void configure(JSch jsch, Session session) throws JSchException {
        session.setPassword(password);
        session.setConfig("PreferredAuthentications", "password,keyboard-interactive,publickey");
    }
}
