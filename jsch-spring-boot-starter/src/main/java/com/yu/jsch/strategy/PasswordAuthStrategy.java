package com.yu.jsch.strategy;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.yu.jsch.HostDefinition;

import java.util.Objects;

/**
 * Password-based authentication strategy.
 */
public class PasswordAuthStrategy implements AuthStrategy {

    private final String password;

    public PasswordAuthStrategy(String password) {
        this.password = Objects.requireNonNull(password, "password must not be null");
    }

    @Override
    public void configure(JSch jsch, Session session, HostDefinition host) throws JSchException {
        session.setPassword(password);
    }
}
