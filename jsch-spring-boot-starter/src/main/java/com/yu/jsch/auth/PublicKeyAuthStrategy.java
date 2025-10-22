package com.yu.jsch.auth;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import java.nio.charset.StandardCharsets;

/**
 * Public key based authentication. Can use a private key file path or inline private key content.
 */
public class PublicKeyAuthStrategy implements AuthStrategy {
    private final String privateKeyPath;
    private final String privateKeyPem;
    private final String passphrase;

    public PublicKeyAuthStrategy(String privateKeyPath, String privateKeyPem, String passphrase) {
        this.privateKeyPath = privateKeyPath;
        this.privateKeyPem = privateKeyPem;
        this.passphrase = passphrase;
    }

    @Override
    public void configure(JSch jsch, Session session) throws JSchException {
        if (privateKeyPath != null && !privateKeyPath.isBlank()) {
            if (passphrase != null) {
                jsch.addIdentity(privateKeyPath, passphrase);
            } else {
                jsch.addIdentity(privateKeyPath);
            }
        } else if (privateKeyPem != null && !privateKeyPem.isBlank()) {
            byte[] keyBytes = privateKeyPem.getBytes(StandardCharsets.UTF_8);
            byte[] passphraseBytes = passphrase != null ? passphrase.getBytes(StandardCharsets.UTF_8) : null;
            jsch.addIdentity("inline", keyBytes, null, passphraseBytes);
        }
        session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
    }
}
